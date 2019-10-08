package com.comers.processor;

import com.comers.annotation.annotation.EventReceiver;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "要开始注解结识了啊");
        //收集所有的注解的信息以及所在类的信息
        collectSubscribers(set, roundEnvironment, processingEnv.getMessager());
        //1. 需要生成一个保存所有需要修改类的文件，留给javassit 好知道需要修改哪些类
        createFile();
//        saveInfo();
        return true;
    }

    private void saveInfo() {
        StringBuffer buffer = new StringBuffer();
        Set<TypeElement> clazzs = methodsByClass.keySet();

        for (TypeElement element : clazzs) {
            buffer.append( "\"" + element.getQualifiedName() + "\"");
        }

        try { // write the file
            JavaFileObject source = filer.createSourceFile("com.example.yore.myannotation.");
            Writer writer = source.openWriter();
            writer.write(buffer.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
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
}
