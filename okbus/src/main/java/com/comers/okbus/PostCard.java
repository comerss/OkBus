package com.comers.okbus;

import java.lang.reflect.Type;

 interface PostCard<T> {
   default public Type getType() {//获取需要解析的泛型T类型
        return new ParameterizedTypeImpl(PostData.class, new Type[]{ClassTypeHelper.findNeedClass(getClass())});
    }
}
