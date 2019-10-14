package com.comers.bus;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.comers.okbus.AbstractHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainActivity_Helper {
    WeakReference target;
    ArrayList<Object> tags = new ArrayList<>();
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap();

    public MainActivity_Helper(MainActivity var1) {
        this.target = new WeakReference(var1);
    }


    public void postchanged(String text) {
        MainActivity to = (MainActivity) target.get();
        if (to instanceof Activity && to.isFinishing()) {
            return;
        }
        if (target.get() != null) {
            to.changed(text);
        }

    }


    public void post(java.lang.Object obj) {
        com.comers.bus.MainActivity to = (com.comers.bus.MainActivity) target.get();
        if (to == null || to instanceof android.app.Activity && ((android.app.Activity) to).isFinishing()) {
            return;
        }
        if (obj.getClass().getName().equals("java.lang.Integer")) {
            to.dataChanged((java.lang.Integer) obj);
        }
        if (obj.getClass().getName().equals("java.lang.String")) {
            to.changed((java.lang.String) obj);
        }
    }

}
