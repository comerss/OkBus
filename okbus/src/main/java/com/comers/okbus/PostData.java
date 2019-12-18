package com.comers.okbus;

public class PostData<T> implements PostCard<T> {
    public T data;

    public PostData(T data) {
        this.data = data;
    }

    public PostData() {
    }

    public Class getDataClass() {
        if (data == null) {
            return Object.class;
        }
        return data.getClass();
    }
}
