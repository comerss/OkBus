package com.comers.okbus;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;

public class OkBus {
    public static OkBus INSTANCE = new OkBus();
    private LinkedHashMap<Class, Object> objDeque = new LinkedHashMap<>();

    private OkBus() {
    }


    public void post(Object text) {

    }

    public void post(Object text, Class... to) {

    }

    public void post(Object text, String... tag) {

    }

    public <T> T post(Class<T> tClass, Object text) {
        return null;
    }

    public <T> T post(Class<T> tClass, Object text, Class to) {
        return null;
    }

    public <T> T post(Class<T> tClass, Object text, String tag) {
        return null;
    }

    public void register(Object target) {

    }
}
