package com.comers.okbus;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class OkBus {
    public static OkBus INSTANCE = new OkBus();
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap<>();

    private OkBus() {
    }


    public void post(Object obj) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            helper.post(obj);
        }
    }

    public void post(Object text, Class... to) {
        for (Class cla : to) {
            objDeque.get(cla).post(text);
        }
    }

    public void post(Object text, String... tag) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
            helper.post(text,tag);
        }
    }
    public <T> T post(Object text, String tag) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
//            helper.post(text,tag);
        }
        return null;
    }

    public <T> T post(Class<T> tClass, Object text, Class to) {
     return objDeque.get(to).post(tClass,text);
    }

    public <T> T post(Class<T> tClass, Object text, String tag) {
        Iterator it = this.objDeque.values().iterator();
        while (it.hasNext()) {
            AbstractHelper helper = (AbstractHelper) it.next();
//            return helper.post(text,tag);
        }
        return null;
    }

    public void register(Object target) {
        if(objDeque.containsKey(target.getClass())){
            return;
        }
    }

    public void unregister(Object target) {
        objDeque.remove(target.getClass());
    }
}
