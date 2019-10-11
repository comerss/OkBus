package com.comers.bus;

import com.comers.okbus.AbstractHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainActivity_Helper {
    MainActivity target;
    ArrayList<Object> tags = new ArrayList<>();
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap();

    public MainActivity_Helper(MainActivity var1) {
        this.target = var1;
    }

    public void postdataChanged(int var1) {
        this.target.dataChanged(var1);
    }

    public void postchanged(String text) {
        target.changed(text);
    }

    public void post(Object obj) {
        for (com.comers.okbus.AbstractHelper helper : objDeque.values()) {
            helper.post(obj);
        }
    }
}
