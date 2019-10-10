package com.comers.annotation.annotation;

import com.comers.annotation.mode.Mode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface EventReceiver {
    int threadMode() default Mode.POST_THREAD;
    Class[] from() default {};
    String[] tags() default {};
}
