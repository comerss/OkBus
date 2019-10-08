package com.comers.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import javax.xml.crypto.dsig.TransformException

class OkBusTransform extends Transform {


    def pool = ClassPool.default
    def project

    OkBusTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "OkBusransform"
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

        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }

//        def  helper=new ProcessorHelper()

        transformInvocation.inputs.each {

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

                FileUtils.copyFile(it.file, dest)
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)
                findTarget(it.file, preFileName)
//                // 获取output目录
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
//        def list=helper.info

        /*for (String str:list){
            *//*def ctClass = pool.get(str)
            def methods = ctClass.getDeclaredMethods()
            for (CtMethod method : methods) {
                if (method.getAnnotations().contains("com.comers.annotation.annotation.EventReceiver")) {
                    CtMethod methodss = CtMethod.make("public void see(String hello){" + "System.out.println(hello);" + "}", ctClass)
                    ctClass.addMethod(methodss)
                }
            }*//*
        }*/

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
        def ctClass = pool.get(name)
        def methods = ctClass.getDeclaredMethods()


        for (CtMethod method : methods) {
            Object[] list = method.getAvailableAnnotations()
            if (list.length > 0) {
                def annotation = list[0].getAt("h").getAt("annotation")
                LinkedHashMap map = annotation.getProperties()
                String typeName = map.get("typeName")
                if ("com.comers.annotation.annotation.EventReceiver".equals(typeName)) {
                    LinkedHashMap members = annotation.getAt("members")
                    Class[] obj = members.get("from").getAt("value")
                    println(members.get("threadMode").getAt("value"))
                }
            }

            /* if (method.getAnnotations().contains("com.comers.annotation.annotation.EventReceiver")) {
                 CtMethod methodss = CtMethod.make("public void see(String hello){" + "System.out.println(hello);" + "}", ctClass)
                 ctClass.addMethod(methodss)
             }*/
        }

    }
}