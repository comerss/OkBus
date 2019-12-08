package com.comers.processor;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.omg.PortableServer.POA;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import de.greenrobot.common.ListMap;

public class OkBusProcessor extends AbstractProcessor {

    private Filer filer;
    private Elements elements;
    private final ListMap<TypeElement, ExecutableElement> methodsByClass = new ListMap<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> hashSet = new HashSet<>();
        hashSet.add(EventReceiver.class.getCanonicalName());
        return hashSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        filer = processingEnvironment.getFiler();
        elements = processingEnvironment.getElementUtils();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "我是一个OkBusProcessor");

    }

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
                    .addStatement("super(target)")
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
                String paramName=method.getParameters().get(0).asType().toString();
                if(paramName.contains("<")&&paramName.contains(">")){
                    initTag.addStatement("paramList.add("+paramName.substring(0,paramName.indexOf("<"))+".class)");
                }else{
                    initTag.addStatement("paramList.add("+paramName+".class)");
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

    private boolean checkHasNoErrors(ExecutableElement element, Messager messager) {
        if (element.getModifiers().contains(Modifier.STATIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "EventReceiver method must not be static", element);
            return false;
        }

        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "EventReceiver method must be public", element);
            return false;
        }

        List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();
        if (parameters.size() != 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "EventReceiver method must have exactly 1 parameter", element);
            return false;
        }
        return true;
    }

    public String getPackage(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }
}
