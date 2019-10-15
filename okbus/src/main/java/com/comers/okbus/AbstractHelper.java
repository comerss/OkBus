package com.comers.okbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AbstractHelper {
    public ArrayList tags = new ArrayList();
    public ExecutorService executors = Executors.newFixedThreadPool(5);
    public ActionHandler handler = new ActionHandler();

    public void post(final Object obj) {

    }

    public void post(Object text, Class... to) {
    }

    public void post(Object text, String... tag) {
    }

    public <T> T post(Class<T> tClass, Object text, Class to) {
        return null;
    }
    public <T> T post(Class<T> tClass, Object text) {
        return null;
    }

    public static class ActionHandler extends Handler {
        public ActionHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
