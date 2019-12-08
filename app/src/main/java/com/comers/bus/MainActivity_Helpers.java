package com.comers.bus;

import android.app.Activity;

import com.comers.okbus.AbstractHelper;
import com.comers.okbus.OkBus;
import com.comers.okbus.PostData;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;

public class MainActivity_Helpers extends AbstractHelper {
    WeakReference target;
    private LinkedHashMap<Class, ? extends AbstractHelper> objDeque = new LinkedHashMap();

    public MainActivity_Helpers(Object obj) {
        super(obj);
    }


    public void postchanged(String text) {
        MainActivity to = (MainActivity) target.get();
        if (to instanceof Activity && to.isFinishing()) {
            return;
        }
        if (target.get() != null) {
            to.changed(text);
        }

        Runnable runnable=new Runnable() {
            @Override
            public void run() {

            }
        };
    }


    public void post(java.lang.Object obj) {
        final OkBusActivity to = (com.comers.bus.OkBusActivity) target.get();
        if (to == null || to instanceof android.app.Activity && ((android.app.Activity) to).isFinishing()) {
            return;
        }
        if(obj instanceof PostData){

        }
        if (obj.getClass().getName().equals("java.lang.String")) {
            final java.lang.Object param = obj;

        }
    }
}

