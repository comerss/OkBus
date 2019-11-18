package com.comers.okbus;

import android.drm.DrmStore;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OkBus {
    private volatile static OkBus INSTANCE;
    //存储每个类对应的辅助类
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap<>();

    private OkBus() {
    }

    public static OkBus getDefault() {
        if (INSTANCE == null) {
            synchronized (OkBus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OkBus();
                }
            }
        }
        return INSTANCE;
    }

    public <T> void post(T obj) {
        Iterator it = this.objDeque.keySet().iterator();
        while (it.hasNext()) {
            Class to = (Class) it.next();
            AbstractHelper helper = (AbstractHelper) objDeque.get(to);
            if (helper != null) {
                helper.post(obj);
            } else {
                objDeque.remove(to);
            }
        }
    }

    //发送给一组
    public <T> void post(T event, Class... to) {
        for (Class cla : to) {
            if (objDeque.containsKey(to)) {
                AbstractHelper helper = objDeque.get(cla);
                if (helper != null) {
                    helper.post(event);
                } else {
                    objDeque.remove(cla);
                }
            }
        }
    }

    //发送给一组 tag
    public <T> void post(T event, String... tag) {
        Iterator it = this.objDeque.keySet().iterator();
        while (it.hasNext()) {
            Class cl = (Class) it.next();
            AbstractHelper helper = (AbstractHelper) objDeque.get(cl);
            if (helper != null) {
                for (String ta : tag) {
                    if (helper.tags.contains(ta)) {
                        helper.post(event, ta);
                    }
                }
            } else {
                objDeque.remove(cl);
            }
        }
    }


    public <T> T post(T tClass, Object text, Class to) {
        AbstractHelper helper = objDeque.get(to);
        if (helper != null) {
            return helper.post(tClass, text);
        } else {
            objDeque.remove(to);
        }
        return null;
    }


    public void register(Object target) {
        if (target != null) {
            if (objDeque.containsKey(target.getClass())) {
                return;
            }
        }
    }

    public void unregister(Object target) {
        if (target != null) {
            if (objDeque.containsKey(target.getClass())) {
                objDeque.remove(target.getClass());
            }
        }
    }

    ExecutorService executors = Executors.newFixedThreadPool(5);
    Handler handler = new Handler(Looper.getMainLooper());

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutors() {
        return executors;
    }

}
