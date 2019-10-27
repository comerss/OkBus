package com.comers.okbus;

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
    static OkBus INSTANCE;
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap<>();

    private OkBus() {
    }

    public static OkBus getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new OkBus();
        }
        return INSTANCE;
    }

    public void post(Object obj) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            helper.post(obj);
        }
    }

    public void post(Object event, Class... to) {
        for (Class cla : to) {
            AbstractHelper helper = objDeque.get(cla);
            if (helper != null) {
                helper.post(event);
            }
        }
    }

    public void post(Object event, String... tag) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            for (String ta : tag) {
                if (helper.tags.contains(ta)) {
                    helper.post(event, tag);
                }
            }
        }
    }


    public <T> T post(Class<T> tClass, Object text, Class to) {
        AbstractHelper helper = objDeque.get(to);
        if (helper != null) {
            return helper.post(tClass, text);
        }
        return null;
    }


    public void register(Object target) {
        if (objDeque.containsKey(target.getClass())) {
            return;
        }
    }

    public void unregister(Object target) {
        objDeque.remove(target.getClass());
    }

    ExecutorService executors = Executors.newFixedThreadPool(5);
    Handler handler = new Handler();

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutor() {
        return executors;
    }
}
