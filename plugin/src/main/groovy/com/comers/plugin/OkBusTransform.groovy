package com.comers.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.squareup.javapoet.CodeBlock
import javassist.*
import javassist.bytecode.annotation.IntegerMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import sun.rmi.runtime.Log

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
                println(it.file.absolutePath+jarName)
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
                copyOkbus(destDir)
                Loader cl = new Loader(pool)
                if (EventReceiver == null) {
                    EventReceiver = cl.loadClass("com.comers.annotation.annotation.EventReceiver")
                }
                //需要修改的文件的集合
                def list = helper.getInfo()
                def registerBuffer = new StringBuffer()
                for (String str : list) {
                    println "============: " + str
                    createNewFile(str, registerBuffer)
                }
                editRegister(registerBuffer)
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
    Class EventReceiver

    private void copyOkbus(String destDir) {
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
    }

    private void createNewFile(String name, StringBuffer buffer) {
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
        println "------------ " + name
        buffer.append($/if(android.text.TextUtils.equals($1.getClass().getName().toString(),/$+"\"" + ctClass.getName() +"\"" + $/)&&!objDeque.containsKey($1.getClass())){/$)
        buffer.append("\n"+ctClass.getName() + "_Helper helper=" + "new " + ctClass.getName() + "_Helper" + "((" + ctClass.getName() + $/)$1);/$)
        buffer.append("\n"+"registerParam(helper);")
        buffer.append("\n"+$/this.objDeque.put($1.getClass(), helper);/$)
        buffer.append("\nreturn;\n")
        buffer.append("}\n")

        println(buffer.toString())


    }

    void editRegister(StringBuffer buffer) {
        // 修改OkBus 来处理对应的能能够调用的方法
        //等遍历完所有的文件之后 我们需要修改 oKbus 来完成事件的真正分发 与 调用
        CtClass okbus = pool.get("com.comers.okbus.OkBus")
        if (okbus.isFrozen()) {
            okbus.defrost()
        }
        buffer = new StringBuffer($/{if ($1 == null || objDeque.contains($1.getClass())) {/$ +
                "            return;\n" +
                "        }\n").append(buffer.toString()).append("}")
        //修改注册方法
        CtMethod register = okbus.getDeclaredMethod("register")
        register.setBody(buffer.toString())
        okbus.writeFile(destDir)
        okbus.detach()
    }

}