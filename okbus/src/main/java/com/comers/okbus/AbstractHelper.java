package com.comers.okbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class AbstractHelper {
    public WeakReference target;
    //存储每个类里面所有的
    public ArrayList tags = new ArrayList();
    //存储每个类对应的参数类型
    public  List<Class> paramList = new ArrayList<>();

    public AbstractHelper(Object obj){
        target=new WeakReference(obj);
    }

    public <T> void post(T obj) {
        post(obj, null);
    }

    public <T> void post(T text, String tag) {
    }

    public <T> T post(T tClass, Object text) {
        return null;
    }

    public boolean checkNull(WeakReference reference) {
        if (reference == null || reference.get() == null) {
            return true;
        }
        final Object to = reference.get();
        if (to == null || to instanceof android.app.Activity && ((android.app.Activity) to).isFinishing()) {
            return true;
        }
        return false;
    }

    public boolean checkTag(String tag, String tag1) {
        if (tag == null || tag.isEmpty() || !tag1.isEmpty() && TextUtils.equals(tag, tag1)) {
            return true;
        }
        return false;
    }

}
