package com.comers.bus;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.comers.okbus.AbstractHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainActivity_Helper extends AbstractHelper {
    WeakReference target;
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

    public void post3(java.lang.Object obj) {
        final com.comers.bus.MainActivity to = (com.comers.bus.MainActivity) target.get();
        if (to == null || to instanceof android.app.Activity && ((android.app.Activity) to).isFinishing()) {
            return;
        }
        if (obj.getClass().getName().equals("java.lang.Integer")) {
            final java.lang.Object param = obj;
            Runnable task = new Runnable() {
                @Override
                public void run() {
                }
            };
            handler.post(task);
        }
        if (obj.getClass().getName().equals("java.lang.String")) {
            final java.lang.Object param = obj;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    to.changed((java.lang.String) param);
                }
            });
        }
    }

    public void post(java.lang.Object obj) {
        final com.comers.bus.OkBusActivity to = (com.comers.bus.OkBusActivity) target.get();
        if (to == null || to instanceof android.app.Activity && ((android.app.Activity) to).isFinishing()) {
            return;
        }
        if (obj.getClass().getName().equals("java.lang.String")) {
            final java.lang.Object param = obj;
            executors.submit(new Runnable() {
                public void run() {
                    to.changed((java.lang.String) param);
                }
            });
        }
    }

}

