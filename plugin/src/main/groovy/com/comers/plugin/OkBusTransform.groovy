package com.comers.plugin

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.IntegerMemberValue
import kotlin.reflect.jvm.internal.impl.builtins.KotlinBuiltIns
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
//                // 获取output目录
                destDir = preFileName
                findTarget(it.file, preFileName)

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
                        throw new IllegalArgumentException("EventReceiver threadMode must be one of Mode")
                    }
                    methodList.add(method)
                }
            }
        }


        //生成对应的辅助文件 作为调用的桥梁
        CtClass newClass = pool.makeClass(getClazzName(ctClass.getName()) + "_Helper")

        //成员变量
        CtField ctField = new CtField(ctClass, "target", newClass)
        newClass.addField(ctField)

        //构造函数
        CtConstructor constructor = CtNewConstructor.make("public " + getClazzName(ctClass.getName()) + " (" + ctClass.getName() + " target){this.target=target;}", newClass)
        newClass.addConstructor(constructor)
        newClass.writeFile(dir.getParent().replace("com.comers.okbus", ""))
        newClass.defrost()


         // 修改OkBus 来处理对应的能能够调用的方法
         //等遍历完所有的文件之后 我们需要修改 oKbus 来完成事件的真正分发 与 调用
         CtClass okbus = pool.get("com.comers.okbus.OkBus")
         if(okbus.isFrozen()){
             okbus.defrost()
         }

         CtMethod register=okbus.getDeclaredMethod("register")
        println(destDir)
         StringBuffer buffer=new StringBuffer()
         buffer.append("if(android.text.TextUtils.equals(target.getClass().getName().toString(),\""+ctClass.getName()+"\")){")
         buffer.append("objDeque.add(")
         buffer.append("new "+newClass.getName()+"(("+ctClass.getName()+")target)")
         buffer.append(");")
         buffer.append("}")
         if(okbus.isFrozen()){
             okbus.defrost()
         }
         register.insertAfter(buffer.toString())
         okbus.writeFile(destDir)
       /* println(destDir)
        CtClass okbus = pool.get("com.comers.okbus.OkBus")
        if (okbus.isFrozen()) {
            okbus.defrost()
        }

        CtField field = okbus.getFields().find {
            return it.type == pool.get("java.lang.String")
        }
        if (field == null) {
            CtField busFiled = new CtField(pool.get("java.lang.String"), "str", okbus)
            okbus.addField(busFiled)
        }
        okbus.writeFile(destDir)*/
        okbus.detach()


    }

//获取文件  带包名 的名字 例如 com.comers.okbus.OkBus
    String getClazzName(String fileName) {
        String name = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())
        return name
    }

}