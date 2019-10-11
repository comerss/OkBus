package com.comers.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.IntegerMemberValue
import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.internal.impldep.org.junit.Assert
import org.omg.CORBA.ObjectHelper
import sun.management.MethodInfo

import javax.xml.crypto.dsig.TransformException

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
                if (!it.file.absolutePath.contains("okbus/build/intermediates/")) {
                    FileUtils.copyFile(it.file, dest)
                }else {

                }
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)
//                // 获取output目录
                destDir = preFileName

                //先清除已经生成的文件 以免造成文件内容重复
                File file = new File(destDir + "/com/comers/okbus")
                println(file.absolutePath + "------>" + file.exists())
                if (file.exists()) {
                    FileUtils.deleteDirectory(file)
                }

                //查询需要修稿的文件
                findTarget(it.file, preFileName)
                //修改 OkBus
                editOkBus()

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


    private void findTarget(File dir, String fileName) {
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName)
            }
        } else {
            modify(dir, fileName)
        }
    }

    private void modify(File dir, String fileName) {

        def filePath = dir.absolutePath

        if (!filePath.endsWith(SdkConstants.DOT_CLASS)) {
            return
        }
        if (filePath.contains('R$') || filePath.contains('R.class')
                || filePath.contains("BuildConfig.class")) {
            return
        }
        def className = filePath.replace(fileName, "")
                .replace("\\", ".")
                .replace("/", ".")

        def name = className.replace(SdkConstants.DOT_CLASS, "").substring(1)

        if (name.startsWith(".")) {
            name = name.substring(1, name.length())
        }

        if (!helper.getInfo().contains(name) || name.endsWith("_Helper")) {
            return
        }

        createNewFile(dir, name)

    }

    private void createNewFile(File dir, String name) {
        CtClass ctClass = pool.get(name)
       /* if (EventReceiver == null) {
            EventReceiver = pool.get("com.comers.annotation.annotation.EventReceiver").toClass()
        }

        if (!ctClass.hasAnnotation(EventReceiver)) {
            ctClass.detach()
            return
        }*/
        if (ctClass.isFrozen()) {
            ctClass.defrost()
        }
        def methods = ctClass.getDeclaredMethods()
        //需要处理的方法
        List<CtMethod> methodList = new ArrayList<>()
        //TODO 需要处理注解的时候把要处理的方法名字带过来，否则这里遍历很耗时间
        for (CtMethod method : methods) {
            Object[] list = method.getAvailableAnnotations()
            if (list.length > 0) {
                def annotation = list[0].getAt("h").getAt("annotation")
                LinkedHashMap map = annotation.getProperties()
                String typeName = map.get("typeName")
                if ("com.comers.annotation.annotation.EventReceiver".equals(typeName)) {
                    LinkedHashMap members = annotation.getAt("members")
                    ArrayMemberValue obj = members.get("from").getAt("value")
                    IntegerMemberValue integerMemberValue = members.get("threadMode").getAt("value")
                    int mode = integerMemberValue.value
                    if (mode < 1 || mode > 4) {
                        throw new IllegalArgumentException(ctClass.name + "  EventReceiver threadMode must be one of Mode， or between 1 and 4 ")
                    }
                    methodList.add(method)
                }
            }
        }

        if (methodList.size() == 0) {
            //如果不需要修改就释放文件
            ctClass.detach()
            return
        }

        //创建统一的接口类
        CtClass absHelper=pool.makeInterface("com.comers.okbus.AbstractHelper")
        CtMethod absPost=CtNewMethod.make("public void post(java.lang.Object obj);",absHelper)
        absHelper.addMethod(absPost)
        absHelper.writeFile(destDir)
        absHelper.defrost()



        //生成对应的辅助文件 作为调用的桥梁
        CtClass helper = pool.makeClass("com.comers.okbus." + getClazzName(ctClass.getName()) + "_Helper")
        CtClass helperSuper = pool.get("com.comers.okbus.AbstractHelper")
//        helper.setSuperclass(helperSuper)
        helper.addInterface(helperSuper)

        //成员变量 目标类，也就是最终调用方法的类
        CtField ctField = new CtField(ctClass, "target", helper)
        helper.addField(ctField)
        //成员变量 存储方法能接受的参数类型


        //构造函数
        CtConstructor constructor = CtNewConstructor.make("public " + getClazzName(ctClass.getName()) + " (" + ctClass.getName() + " target){this.target=target;}", helper)
        helper.addConstructor(constructor)

        //构造post方法 并进行事件分发
        StringBuffer postBody = new StringBuffer()
        postBody.append("public void post(java.lang.Object obj){")
        for (CtMethod ctMethod : methodList) {
            postBody.append("if(obj.getClass().getName().equals(" + "\"" + ctMethod.getParameterTypes()[0].name + "\"" + ")){")
            postBody.append("this.target." + ctMethod.name + "((" + ctMethod.getParameterTypes()[0].name.toString() + ")obj);}")
        }

        postBody.append("}")
        println(postBody.toString())
        CtMethod post = CtNewMethod.make(postBody.toString(), helper)
        helper.addMethod(post)
        helper.writeFile(destDir)
        helper.defrost()

        // 修改OkBus 来处理对应的能能够调用的方法
        //等遍历完所有的文件之后 我们需要修改 oKbus 来完成事件的真正分发 与 调用
        CtClass okbus = pool.get("com.comers.okbus.OkBus")
        if (okbus.isFrozen()) {
            okbus.defrost()
        }


        //修改注册方法
        CtMethod register = okbus.getDeclaredMethod("register")
        StringBuffer buffer = new StringBuffer()
        buffer.append("if(android.text.TextUtils.equals(target.getClass().getName().toString(),\"" + ctClass.getName() + "\")){")
        buffer.append("objDeque.put(target.getClass(),")
        buffer.append("new " + helper.getName().toString() + "((" + ctClass.getName() + ")target)")
        buffer.append(");")
        buffer.append("}")
        register.insertAfter(buffer.toString())

    }


    private void editOkBus(){
        // 修改OkBus 来处理对应的能能够调用的方法
        //等遍历完所有的文件之后 我们需要修改 oKbus 来完成事件的真正分发 与 调用
        CtClass okbus = pool.get("com.comers.okbus.OkBus")
        if (okbus.isFrozen()) {
            okbus.defrost()
        }

        //修改post方法
        CtMethod pos = okbus.getDeclaredMethod("post")
        String posEdit="java.util.Iterator var2 = this.objDeque.values().iterator();\n" +
                "\n" +
                "        while(var2.hasNext()) {\n" +
                "        com.comers.okbus.AbstractHelper  helper = (com.comers.okbus.AbstractHelper)var2.next();\n" +
                /helper.post($1);/ +
                "        }"

        pos.insertAfter(posEdit)
        okbus.writeFile(destDir)
        okbus.detach()
    }



    String getClazzName(String fileName) {
        String name = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())
        return name
    }

    /*private static ClassLoader getLocaleClassLoader() throws Exception {
        List<URL> classPathURLs = new ArrayList<>();
        // 加载.class文件路径
        classPathURLs.add(classesPath.toURI().toURL());

        // 获取所有的jar文件
        File[] jarFiles = libPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        Assert.assertFalse(ObjectHelper.isArrayNullOrEmpty(jarFiles));

        // 将jar文件路径写入集合
        for (File jarFile : jarFiles) {
            classPathURLs.add(jarFile.toURI().toURL());
        }

        // 实例化类加载器
        return new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]));
    }*/

}