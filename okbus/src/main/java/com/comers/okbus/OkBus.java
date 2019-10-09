package com.comers.okbus;

import java.util.LinkedHashMap;

public class OkBus {
    public static OkBus INSTANCE = new OkBus();
    private LinkedHashMap<Class, Object> objectLinkedHashMap = new LinkedHashMap<>();

    private OkBus() {
    }

    public void post(String text) {

    }

    public void post(String text, Class to) {

    }

    public void register(Object target) {
        objectLinkedHashMap.put(target.getClass(), target);
    }
}
