package com.comers.processor;

import com.comers.annotation.annotation.EventReceiver;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"我是一个OkBusProcessor");
        createFile();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"要开始注解结识了啊");

        collectSubscribers(set,roundEnvironment,processingEnv.getMessager());

        return true;
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
                        //收集注解所在类的信息
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@EventReceiver is only valid for methods", element);
                }
            }
        }
    }

    private void createFile() {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"创建一个文件");
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("ProcessorHelper")
                .addModifiers(Modifier.PUBLIC);

        MethodSpec constructer = MethodSpec.methodBuilder("ProcessorHelper")
                .addModifiers(Modifier.PUBLIC)
                .build();

        JavaFile javaFile = JavaFile.builder("com.comers.processor", classBuilder.addMethod(constructer).build()).build();

        try {
            javaFile.writeTo(new File("/Volumes/Work/works/OkBus/processor/src/main/java"));
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
