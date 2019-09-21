package com.comers.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class ComersPlugin extends Transform implements Plugin<Project> {
    ClassPool classPool = ClassPool.getDefault()

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)
    }

    @Override
    String getName() {
        return "com.comers.plugin.ComersPlugin"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        //插件作用时机
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //插件作用范围
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量更新
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        print("我要真的起飞了！")
        def inputs = transformInvocation.inputs
        TransformOutputProvider provider = transformInvocation.getOutputProvider()
        //如果不是增量更新则删除之前生成的文件
        if (!transformInvocation.incremental) {
            provider.deleteAll()
        }
        inputs.forEach({
            it.directoryInputs.forEach {
                handleDirectory(it, provider)
            }
            it.jarInputs.forEach {
                handleJar(it, provider)
            }

        })

    }
    //使用javassit为添加注解的生成以及实现接口
    void handleDirectory(DirectoryInput input, TransformOutputProvider provider) {
        if (input.file.isDirectory()) {
            input.file.eachFileRecurse {
                classPool.appendClassPath(it.absolutePath)
                print(it.absolutePath)
            }
        }

    }

    void handleJar(JarInput input, TransformOutputProvider provider) {
        if (input.name.endsWith(".jar")) {
            def jarName = input.name
            def md5Name = DigestUtils.md5Hex(input.file.getAbsolutePath())
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4)
            }
            def jarFile = new JarFile(input.file)
            def enumeration = jarFile.entries()
            def tempFile = new File(input.file.getParent() + File.separator + "temp.jar")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            def outputStream = new JarOutputStream(new FileOutputStream(tempFile))
            while (enumeration.hasMoreElements()) {
                def jarEntry = enumeration.nextElement()
                def entryName = jarEntry.name
                def zipEntry = new ZipEntry(entryName)
                def inputStream = jarFile.getInputStream(zipEntry)
                if (checkClassFile(entryName)) {
                }
            }
            outputStream.close()
            jarFile.close()
            def dest = provider.getContentLocation(jarName + md5Name, input.contentTypes, input.scopes, Format.JAR)
            FileUtils.copyDirectory(tempFile, dest)
            tempFile.delete()
        }
    }

    /**
     * 检查class文件是否需要处理
     * @param fileName
     * @return
     */
    boolean checkClassFile(String name) {
        //只处理需要的class文件
        return (name.endsWith(".class") && !name.startsWith("R\$")
                && !"R.class".equals(name) && !"BuildConfig.class".equals(name))
    }
}

