package com.comers.okbus;

public interface AbstractHelper {
    public void post(Object obj);

    public void post(Object text, Class... to);

    public void post(Object text, String... tag);

    public <T> T post(Class<T> tClass, Object text);

    public <T> T post(Class<T> tClass, Object text, Class to);

    public <T> T post(Class<T> tClass, Object text, String tag);
}
