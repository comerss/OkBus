package com.comers.okbus;

public  class PostData<T> implements PostCard<T> {
   public T data;

    public PostData(T data) {
        this.data = data;
    }
    public PostData(){}

    public Class getDataClass(){
        return data.getClass();
    }
}
