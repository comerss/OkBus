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
    //经过我的实验发现这个功能使用的相对较少，因为可能很少关心谁会调用，所以暂时放弃此功能，如果需要增加也很方便
//    Class[] from() default {};
    String tag() default "";
}
