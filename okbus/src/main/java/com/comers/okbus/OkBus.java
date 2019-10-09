package com.comers.okbus;

import android.text.TextUtils;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;

public class OkBus {
    public static OkBus INSTANCE = new OkBus();
    private LinkedHashMap<Class, Object> objectLinkedHashMap = new LinkedHashMap<>();
    private ArrayDeque<Object> objDeque=new ArrayDeque<>();
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
