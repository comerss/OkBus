package com.comers.okbus;

import android.drm.DrmStore;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OkBus {
    private static OkBus INSTANCE;
    //存储每个类对应的辅助类
    ConcurrentHashMap<Class, AbstractHelper> objDeque = new ConcurrentHashMap<>();
    //参数 对应辅助类
    ConcurrentHashMap<Class, List<AbstractHelper>> paramDeque = new ConcurrentHashMap<>();

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
        if (obj == null) {
            return;
        }
        List<AbstractHelper> helperList = paramDeque.get(obj.getClass());
        if(helperList==null){
            return;
        }
        Iterator it = helperList.iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            if (checkHelper(helper)) {
                helper.post(obj);
            }else{
                removeHelper(helper);
            }
        }
    }

    //发送给一组
    public <T> void post(T event, Class... to) {
        for (Class cla : to) {
            if (objDeque.containsKey(to)) {
                AbstractHelper helper = objDeque.get(cla);
                if (checkHelper(helper)) {
                    helper.post(event);
                } else {
                    objDeque.remove(cla);
                }
            }
        }
    }

    //发送给一组 tag
    public <T> void post(T event, String... tag) {
        if (event == null) {
            return;
        }
        List<AbstractHelper> helperList = paramDeque.get(event.getClass());
        if(helperList==null){
            return;
        }
        Iterator it = helperList.iterator();
        while (it.hasNext()) {
            Class cl = (Class) it.next();
            AbstractHelper helper = (AbstractHelper) objDeque.get(cl);
            if (checkHelper(helper)) {
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
        if (checkHelper(helper)) {
            return helper.post(tClass, text);
        } else {
            objDeque.remove(to);
        }
        return null;
    }


    public void register(Object target) {

    }

    public void unregister(Object obj) {
        if (obj != null && objDeque.contains(obj.getClass())) {
            AbstractHelper helper = objDeque.get(obj.getClass());
            if (helper != null) {
                for (Class cls : helper.paramList) {
                    paramDeque.get(cls).remove(helper);
                }
            }
            objDeque.remove(obj.getClass());
        }
    }

    ExecutorService executors = Executors.newCachedThreadPool();
    Handler handler = new Handler(Looper.getMainLooper());

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutors() {
        return executors;
    }

    void registerParam(AbstractHelper helper) {
        if (helper != null) {
            for (Class cls : helper.paramList) {
                List<AbstractHelper> helperList = paramDeque.get(cls);
                if (helperList == null) {
                    helperList = new ArrayList<>();
                }
                helperList.add(helper);
            }
        }
    }

    boolean checkHelper(AbstractHelper helper) {
        if (helper != null && helper.target != null && helper.target.get() != null) {
            return true;
        }
        return false;
    }
    private void removeHelper(AbstractHelper helper) {
        if(helper!=null){

        }
    }
}
