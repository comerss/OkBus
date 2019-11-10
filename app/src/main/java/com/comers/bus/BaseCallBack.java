package com.comers.bus;

import com.comers.okbus.ClassTypeHelper;
import com.comers.okbus.ParameterizedTypeImpl;

import java.lang.reflect.Type;

/**
 * Created by Comers on 2017/10/25.
 */

public abstract class BaseCallBack<T> {
    public Type getType() {//获取需要解析的泛型T类型
        return new ParameterizedTypeImpl(HttpResult.class, new Type[]{ClassTypeHelper.findNeedClass(getClass())});
    }

    public Type getRawType() {//获取需要解析的泛型T raw类型
        return ClassTypeHelper.findRawType(getClass());
    }
}
