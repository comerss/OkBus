package com.comers.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import javassist.bytecode.annotation.IntegerMemberValue
import javassist.bytecode.annotation.StringMemberValue
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

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