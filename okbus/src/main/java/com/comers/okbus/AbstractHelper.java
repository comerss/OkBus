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

    public <T> void post(T obj) {

    }

    public <T> void post(T text, String tag) {
    }

    public <T> T post(Class<T> tClass, Object text) {
        return null;
    }


}
