package com.comers.okbus;

import java.util.HashMap;
import java.util.Map;

public class OkBus {
    private OkBus() {
    }

    private OkBus INSTANCE = new OkBus();

    public OkBus getDefault() {
        return INSTANCE;
    }

    private Map<Class, Object> interfaces = new HashMap<>();

    public void register(Object target) {
        interfaces.put(target.getClass(), target);
    }

    public void post(Object data, Class to) {

    }

}
