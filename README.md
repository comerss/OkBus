# OkBus 

## Eventbus
android里面我们都知道EventBus事件总线 来处理我们的事件分发，确实从一定程度上解决了android的一些通信问题，但是其自身的弊端也不容忽视，在我看来主要是两点

1.EventBus 大量使用后造成混乱，你根本不知道你这个事件会从哪里调用，无法根据自己打的标记进行调用，比如给某个方法打个标记方便识别以及调用。也就是说我们要清晰发给谁 以及方法本身能接受谁

2.EventBus本身使用的是反射的技术，耗时平均大于100ms。虽然对于很多架构而言反射是利器，但是性能的损耗也不可姑息，能否不用反射？

处于对以上问题的思考，我尝试着用市面上已有的技术打造一款解决EventBus问题的事件总线框架。其目的是能够更清晰的使用，不造成使用后的混乱，另一方面想达到直接调用方法的效果而不是反射调用。以下我将使用**APT 、动态生成代码 以及动态修改字节码** 技术来尝试解决这个问题
## 上路吧OkBus

-------
**github 地址** [项目地址](https://github.com/comerss/OkBus.git)
-------

问题既然清楚了，那我们就逐一解决。

一 . 首先不必要考虑新的架构，而是如何在EventBus本身上解决第一个问题，最初的想法是，在注解里增加fromType控制谁能调用，在post方法是增加toType解决到底发给谁还是给一组谁，post也可以增加一个tag根据tag来区分到底发给谁，前提是注解里添加tag。其实这样的思路完全是可以更好的优化EventBus的，能更清晰的看到,大致如下

```
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Subscriber {
    int threadMode() default Mode.POST_THREAD;
    //接受谁 从某种程度来说就是确定关系，我只接受某些类的事件，但是需要提前在我这里声明某个不予响应
    Class[] from() default {};
    //发给谁 我们也可以根据tag分组，比如refresh，这样post 的时候带上这个tag，所有的相同tag的都能收到事件 可以说是分组功能，他决定了 发给谁
    String tag() default "";
}
//发给谁
EventBus.getDefault().post("我来自伽马星球",HomeActivity.class);
```
从上面简短的代码中基本可以看到我的思路了，也就是解决 谁可以调 以及调用谁的问题。

二. 以上思想可以从EventBus本身解决，然而如果想不用反射 EventBus就歇菜了，他无法做到这点。从目前的技术来看，可能解决的技术就是 _动态生成代码 以及动态修改 字节码技术_ 

**思路决定出路**
1. 我们要实现类似于代码直接调用的方式前提是拿到对象，也就是注册的时候我们需要引用到对应的对像，以便于后期调用目标方法
2. 通过register这一步我们是可以拿到对象，但是如何存储以及如何在事件分发的时候 有了对象如何辨别该调用哪个方法？
3. 显然2中调用哪个方法是完全不知道的，我们不能直接写在框架列吧，那如何搞？答案是动态生成。
4. ButterKnife是否还记得，它是如何通过注解来实现调用点击事件监听的？
5. 对于OkBus本身，他就是写死的，比如注册我们志存出一个对象是不够的的，那能不能像ButterKnife那样动态生成辅助类？可以是可以但你Okbus本身这个类写死的如何改？ 动态字节码修改罗

有了以上问题的思考，我们逐一解决对应的问题即可。当然对于我而言上面的东西只不过是事后总结的，事先根本一脸懵逼，无数次脑海里假想和实践才能得出来的。

## 代码实践
### OkBus 类

```
public class OkBus {
    private volatile static OkBus INSTANCE;
    //存储每个类对应的辅助类
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap<>();

    private OkBus() {
    }

    public static OkBus getDefault() {
        if (INSTANCE == null) {
            synchronized (OkBus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OkBus();
                }
            }
        }
        return INSTANCE;
    }

    public <T> void post(T obj) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            helper.post(obj);
        }
    }
    //发送给一组
    public <T> void post(T event, Class... to) {
        for (Class cla : to) {
            AbstractHelper helper = objDeque.get(cla);
            if (helper != null) {
                helper.post(event);
            }
        }
    }
    //发送给一组 tag
    public <T> void post(T event, String... tag) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            for (String ta : tag) {
                if (helper.tags.contains(ta)) {
                    helper.post(event, ta);
                }
            }
        }
    }

    
    public <T> T post(T tClass, Object text, Class to) {
        AbstractHelper helper = objDeque.get(to);
        if (helper != null) {
            return helper.post(tClass, text);
        }
        return null;
    }


    public void register(Object target) {
        if (objDeque.containsKey(target.getClass())) {
            return;
        }
    }

    public void unregister(Object target) {
        if (objDeque.containsKey(target.getClass())) {
            objDeque.remove(target.getClass());
        }
    }

    ExecutorService executors = Executors.newFixedThreadPool(5);
    Handler handler = new Handler(Looper.getMainLooper());

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutors() {
        return executors;
    }

}
```
### APT 以及动态生成代码
我们在这里要实现的是，通过动态生成代码生成辅助类，为了方便调用，我们定义一个抽象类，因为每个类对应的方法不同。

注解定义

```
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface EventReceiver {
    //线程
    int threadMode() default Mode.POST_THREAD;
//    Class[] from() default {};
    //方法分组名 或者称之为方法标记
    String tag() default "";
}
```

第二阶段最主要的就是辅助类的生成（用空间换时间 不反射得多文件），具体的思考细节就不多说了，通过注解我们需要生成每个类对应的辅助文件。（事实上 在实际项目中我们用的最多的是页面级的数据传递，或者调用，所以我认为不会特别多，你不可能每个页面都需要和其他页面通信，所以生成的文件也是有限的，不会大规模）

这里主要做了三件事
1.手机所有使用okbus的类，并生成文件保存留给后面做字节码修改使用
2.辅助类要生成两种方法，第一个是对原有方法的包装 也就是对注解的处理比如线程以及调用
3.我们调用的前提是需要匹配 post 的所带的条件 以及方法本身注解所带有的条件是否吻合，决定是否调用，条件包括fromType tag方法参数类型 的条件校验
```
 @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.size() > 0) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "要开始注解结识了啊");
            //收集所有的注解的信息以及所在类的信息
            collectSubscribers(set, roundEnvironment, processingEnv.getMessager());
            //1. 需要生成一个保存所有需要修改类的文件，留给javassit 好知道需要修改哪些类
            createFile();
            //2. 创建对应的Helper类，本来是javassit创建方便些，但是由于javassit不能处理handler 无法解决只能前移到这里
            createHelper();
        }
        return false;
    }

    int count = 0;

    private void createHelper() {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "--------" + count);
        for (TypeElement element : methodsByClass.keySet()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "我是一个" + element.getSimpleName() + "_Helper" + count++);
            //构造函数
            TypeSpec.Builder helper = TypeSpec.classBuilder(element.getSimpleName() + "_Helper");
            helper.addModifiers(Modifier.PUBLIC);
            helper.superclass(ClassName.get("com.comers.okbus", "AbstractHelper"));
            helper.addField(ClassName.get("java.lang.ref", "WeakReference"), "target", Modifier.PRIVATE);
            helper.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(ClassName.get(getPackage(element.getQualifiedName().toString()), element.getSimpleName().toString()), "target")
                    .addStatement("this.target=new $T(target)", ClassName.get("java.lang.ref", "WeakReference"))
                    .addStatement("initTag()")
                    .addModifiers(Modifier.PUBLIC)
                    .build());


            //post 事件分发函数
            MethodSpec.Builder post = MethodSpec.methodBuilder("post")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.OBJECT, "obj")
                    .addParameter(String.class,"tag")
                    ;
            post.addStatement("if (checkNull(target)) {return;}");

            //tag初始化
            MethodSpec.Builder initTag = MethodSpec.methodBuilder("initTag");

            List<ExecutableElement> methods = methodsByClass.get(element);
            StringBuffer body = new StringBuffer();
            StringBuffer buffer = new StringBuffer();
            boolean containsFans = false;
            boolean containsNormal = false;
            buffer.append("if(obj instanceof com.comers.okbus.PostData){\n");
            for (int i = 0; i < methods.size(); i++) {
                ExecutableElement method = methods.get(i);
                String prefix = "";
                if (i != 0) {
                    prefix = "else ";
                }
                EventReceiver receiver = method.getAnnotation(EventReceiver.class);

                if((receiver.threadMode()== Mode.BACKGROUND||receiver.threadMode()==Mode.NEW_THREAD)&&method.getReturnType()== TypeName.VOID){

                }


                TypeName typeName = ClassName.get(method.getParameters().get(0).asType());
                String name = typeName.toString();

                //先对每个方法生成对应的调用方法
                MethodSpec.Builder mBuild = MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .addParameter(ClassName.get(method.getParameters().get(0).asType()), "obj")
                        ;
                StringBuffer mBody = new StringBuffer();
                mBody.append(" if(checkNull(target)){return;}\n");
                mBody.append("final " + element.getQualifiedName().toString() + " to=(" + element.getSimpleName().toString() + ")target.get();\n");

                if (name.contains("<") && name.contains(">")) {
                    if (receiver != null) {
                        if (receiver.threadMode() == 1) {
                            mBody.append("com.comers.okbus.OkBus.getDefault().getHandler().post(new Runnable() {\n" +
                                    "       @Override\n" +
                                    "        public void run() {\n" +
                                    "        to." + method.getSimpleName() + "(obj);\n" +
                                    "      }\n" +
                                    "  });");
                        } else if (receiver.threadMode() == 2 || receiver.threadMode() == 3) {
                            mBody.append("com.comers.okbus.OkBus.getDefault().getExecutors().submit(new Runnable() {\n" +
                                    "       @Override\n" +
                                    "        public void run() {\n" +
                                    "        to." + method.getSimpleName() + "(obj);\n" +
                                    "        }\n" +
                                    "});");
                        } else {
                            mBody.append("to." + method.getSimpleName() + "(obj);\n");
                        }
                    }
                } else {
                    if (receiver != null) {
                        if (receiver.threadMode() == 1) {
                            mBody.append("com.comers.okbus.OkBus.getDefault().getHandler().post(new Runnable() {\n" +
                                    "      @Override\n" +
                                    "     public void run() {\n" +
                                    "     to." + method.getSimpleName() + "(obj);\n" +
                                    "      }\n" +
                                    "});");
                        } else if (receiver.threadMode() == 2 || receiver.threadMode() == 3) {
                            mBody.append("com.comers.okbus.OkBus.getDefault().getExecutors().submit(new Runnable() {\n" +
                                    "     @Override\n" +
                                    "     public void run() {\n" +
                                    "     to." + method.getSimpleName() + "(obj);\n" +
                                    "     }\n" +
                                    "});");
                        } else {
                            mBody.append("to." + method.getSimpleName() + "(obj);\n");
                        }
                    }
                }
                mBuild.addStatement(mBody.toString());
                helper.addMethod(mBuild.build());


                //事件分发 以及条件校验
                if (name.contains("<") && name.contains(">")) {
                    String posName = "pos" + i;
                    buffer.append("   com.comers.okbus.PostData<" + name + "> " + posName + "=new com.comers.okbus.PostData<" + name + ">(){};\n");
                    buffer.append(containsFans ? "else" : "" + "  if (com.comers.okbus.ClassTypeHelper.equals(((com.comers.okbus.PostData) obj).getType(), " + posName + ".getType())&&checkTag(tag,\""+receiver.tag()+"\")) {\n");
                    containsFans = true;
                    buffer.append("  " + method.getSimpleName() + "((" + method.getParameters().get(0).asType().toString() + ")((com.comers.okbus.PostData) obj).data);");
                    buffer.append("}\n");
                } else {
                    containsNormal = true;
                    body.append(prefix + " if(obj.getClass().equals(" + method.getParameters().get(0).asType().toString() + ".class)&&checkTag(tag,\""+receiver.tag()+"\")){\n");
                    body.append("  " + method.getSimpleName() + "((" + method.getParameters().get(0).asType().toString() + ")obj);");
                    body.append("}\n");
                }

                if (receiver.tag() != null && !receiver.tag().isEmpty()) {
                    initTag.addStatement("tags.add(\"" + receiver.tag() + "\")");
                }
            }
            buffer.append("}");
            if (containsNormal) {
                if(!body.toString().startsWith("else")){
                    buffer.append("else ");
                }
                buffer.append(body.toString());
            }
            post.addStatement(buffer.toString());

            helper.addMethod(post.build());
            helper.addMethod(initTag.build());
//            helper.addMethod(postReturn.build());
            JavaFile javaFile = JavaFile.builder(getPackage(element.getQualifiedName().toString()), helper.build()).build();

            try {
                javaFile.writeTo(filer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void collectSubscribers(Set<? extends TypeElement> annotations, RoundEnvironment env, Messager messager) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                //判断类型是方法的类型
                if (element instanceof ExecutableElement) {
                    ExecutableElement method = (ExecutableElement) element;
                    //检测方法是否符合标准
                    if (checkHasNoErrors(method, messager)) {
                        TypeElement classElement = (TypeElement) method.getEnclosingElement();
                        methodsByClass.putElement(classElement, method);
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@EventReceiver is only valid for methods", element);
                }
            }
        }
    }

    private void createFile() {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "创建一个文件---->ProcessorHelper");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("ProcessorHelper")
                .addModifiers(Modifier.PUBLIC);
        ClassName list = ClassName.get("java.util", "ArrayList");
        MethodSpec constructer = MethodSpec.methodBuilder("ProcessorHelper")
                .addModifiers(Modifier.PUBLIC)
                .build();
        MethodSpec.Builder getInfo = MethodSpec.methodBuilder("getInfo")
                .returns(ArrayList.class)
                .addModifiers(Modifier.PUBLIC);

        CodeBlock.Builder getInfoBlock = CodeBlock.builder()
                .add("$T list = new $T();\n", list, list);
        Set<TypeElement> clazzs = methodsByClass.keySet();

        for (TypeElement element : clazzs) {
            getInfoBlock.add("list.$N(" + "\"" + element.getQualifiedName() + "\"" + ");\n", "add");
        }

        getInfoBlock.add("return list");

        getInfo.addStatement(getInfoBlock.build());

        classBuilder.addMethod(getInfo.build());
//        classBuilder.addMethod(constructer);
        JavaFile javaFile = JavaFile.builder("com.comers.plugin", classBuilder.build()).build();

        try {
            javaFile.writeTo(new File("/Volumes/Work/works/OkBus/plugin/src/main/groovy/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```
生成的文件如下

用来存储哪些类用了okbus
```
public class ProcessorHelper {
  public ArrayList getInfo() {
    ArrayList list = new ArrayList();
        list.add("com.comers.bus.MainActivity");
        list.add("com.comers.bus.OkBusActivity");
        return list;
  }
}
```
辅助类如下

```
public class OkBusActivity_Helper extends AbstractHelper {
  private WeakReference target;

  public OkBusActivity_Helper(OkBusActivity target) {
    this.target=new WeakReference(target);
    initTag();
  }
    原有方法的包装以及调用
  void changed(OkData<Success> obj) {
     if(checkNull(target)){return;}
        final com.comers.bus.OkBusActivity to=(OkBusActivity)target.get();to.changed(obj);
        ;
  }

  void change(String obj) {
     if(checkNull(target)){return;}
        final com.comers.bus.OkBusActivity to=(OkBusActivity)target.get();com.comers.okbus.OkBus.getDefault().getExecutors().submit(new Runnable() {
             @Override
             public void run() {
             to.change(obj);
             }
        });;
  }
    //事件分发 以及条件校验 决定调用哪个方法
  public void post(Object obj, String tag) {
    if (checkNull(target)) {return;};
    if(obj instanceof com.comers.okbus.PostData){
           com.comers.okbus.PostData<com.comers.bus.OkData<com.comers.bus.Success>> pos0=new com.comers.okbus.PostData<com.comers.bus.OkData<com.comers.bus.Success>>(){};
          if (com.comers.okbus.ClassTypeHelper.equals(((com.comers.okbus.PostData) obj).getType(), pos0.getType())&&checkTag(tag,"OkActivity")) {
          changed((com.comers.bus.OkData<com.comers.bus.Success>)((com.comers.okbus.PostData) obj).data);}
        }else  if(obj.getClass().equals(java.lang.String.class)&&checkTag(tag,"")){
          change((java.lang.String)obj);}
        ;
  }
  //tag收集 会在初始化类的时候初始化到集合里面
  void initTag() {
    tags.add("OkActivity");
  }
}

```
### 动态修改字节码
看上面的代码，我们不难发现OkBus 的注册方法咩有具体的实现，因为注册的时候辅助类还没有，没法注册到objDeque。这个就需要我们动态的去修改了 也就是根据之前我们生成的ProcessorHelper 存储的需要修改的类来进行注册就行了

不知道如何自定义插件的需要问娘哦。


```
class OkBusTransform extends Transform {


    def pool = ClassPool.default
    def project
    def helper = new ProcessorHelper()
    String destDir

    OkBusTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "OkBusTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        pool.clearImportedPackages()
        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }
        transformInvocation.inputs.each {
            //这里是三方的jar包以及 我们的moudle打成的jar包 ，比如我们的okbus 打成的jar包
            it.jarInputs.each {
                pool.insertClassPath(it.file.absolutePath)
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = it.name
                def md5Name = DigestUtils.md5Hex(it.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarName + md5Name, it.contentTypes, it.scopes, Format.JAR)
                //不能copy okbus 的jar包 因为后面修改的时候没法覆盖jar包里面的Okbus造成 有两个okbus 造成冲突
                //TODO 如果打成jar包的话怎么让他不copy  现在还在项目中能这么搞，以后打成jar  这里需要修改
                if (!it.file.absolutePath.contains("okbus/build/intermediates/")) {
                    FileUtils.copyFile(it.file, dest)
                } else {

                }
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)
//                // 获取output目录
                destDir = preFileName
                //查询需要修稿的文件
//                findTarget(it.file, preFileName)
                //修改 OkBus
//                editOkBus()
                //先清除已经生成的文件 以免造成文件内容重复
                File file = new File(destDir + "/com/comers/okbus")
                println(file.absolutePath + "------>" + file.exists())
                if (file.exists()) {
                    FileUtils.deleteDirectory(file)
                }
                //将okbus 目录下的类不会被写到目标目录所以需要手动写到目录
                CtClass absHelper = pool.get("com.comers.okbus.AbstractHelper")
                absHelper.defrost()
                absHelper.writeFile(destDir)

                CtClass ClassTypeHelper = pool.get("com.comers.okbus.ClassTypeHelper")
                ClassTypeHelper.defrost()
                ClassTypeHelper.writeFile(destDir)

                CtClass postCard = pool.get("com.comers.okbus.PostCard")
                postCard.defrost()
                postCard.writeFile(destDir)

                CtClass ParameterizedTypeImpl = pool.get("com.comers.okbus.ParameterizedTypeImpl")
                ParameterizedTypeImpl.defrost()
                ParameterizedTypeImpl.writeFile(destDir)

                CtClass PostData = pool.get("com.comers.okbus.PostData")
                PostData.defrost()
                PostData.writeFile(destDir)
                Loader cl = new Loader(pool)
                if (EventReceiver == null) {
                    EventReceiver = cl.loadClass("com.comers.annotation.annotation.EventReceiver")
                }
                //需要修改的文件的集合
                def list = helper.getInfo()
                for (String str : list) {
                    createNewFile(str)
                }

                def dest = transformInvocation.outputProvider.getContentLocation(
                        it.name,
                        it.contentTypes,
                        it.scopes, Format.DIRECTORY)
                println "copy directory: " + it.file.absolutePath
                println "dest directory: " + dest.absolutePath
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(it.file, dest)
            }

        }

        pool.clearImportedPackages()
    }
    
    private void createNewFile(String name) {
        CtClass ctClass = pool.get(name)
        if (ctClass.isFrozen()) {
            ctClass.defrost()
        }
        def methods = ctClass.getDeclaredMethods()
        //需要处理的方法
        List<CtMethod> methodList = new ArrayList<>()
        //TODO 需要处理注解的时候把要处理的方法名字带过来，否则这里遍历很耗时间
        for (CtMethod method : methods) {
            def ano = method.getAnnotation(EventReceiver)
            if (ano != null) {
                def annotation = ano.getAt("h").getAt("annotation")
                LinkedHashMap members = annotation.getAt("members")
                IntegerMemberValue integerMemberValue = members.get("threadMode").getAt("value")
                int mode = integerMemberValue.value
                if (mode < 1 || mode > 4) {
                    throw new IllegalArgumentException(ctClass.name + "  EventReceiver threadMode must be one of Mode， or between 1 and 4 ")
                }
                methodList.add(method)
            }
        }

        if (methodList.size() == 0) {
            //如果不需要修改就释放文件
            ctClass.detach()
            return
        }


        // 修改OkBus 来处理对应的能能够调用的方法
        //等遍历完所有的文件之后 我们需要修改 oKbus 来完成事件的真正分发 与 调用
        CtClass okbus = pool.get("com.comers.okbus.OkBus")
        if (okbus.isFrozen()) {
            okbus.defrost()
        }


        //修改注册方法
        CtMethod register = okbus.getDeclaredMethod("register")
        StringBuffer buffer = new StringBuffer()
        buffer.append("if(android.text.TextUtils.equals(target.getClass().getName().toString(),\"" + ctClass.getName() + "\")&&!objDeque.containsKey(target.getClass())){")
        buffer.append("objDeque.put(target.getClass(),")
        buffer.append("new " + ctClass.getName() + "_Helper" + "((" + ctClass.getName() + ")target)")
        buffer.append(");")
        buffer.append("}")
        register.insertAfter(buffer.toString())
        okbus.writeFile(destDir)

    }
    
}
```
