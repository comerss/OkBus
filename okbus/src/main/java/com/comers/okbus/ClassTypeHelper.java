package com.comers.okbus;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by Comers on 2017/10/28.
 */

public class ClassTypeHelper {
    /**
     * find the type by interfaces
     *
     * @param cls
     * @param <R>
     * @return
     */
    public static <R> Type findNeedType(Class<R> cls) {
        List<Type> typeList = getMethodTypes(cls);
        if (typeList == null || typeList.isEmpty()) {
            return Object.class;
        }
        return typeList.get(0);
    }

    /**
     * MethodHandler
     */
    public static <T> List<Type> getMethodTypes(Class<T> cls) {
        Type typeOri = cls.getGenericSuperclass();
        List<Type> needtypes = null;
        // if Type is T
        if (typeOri instanceof ParameterizedType) {
            needtypes = new ArrayList<>();
            Type[] parentypes = ((ParameterizedType) typeOri).getActualTypeArguments();
            for (Type childtype : parentypes) {
                needtypes.add(childtype);
                if (childtype instanceof ParameterizedType) {
                    Type[] childtypes = ((ParameterizedType) childtype).getActualTypeArguments();
                    for (Type type : childtypes) {
                        needtypes.add(type);
                    }
                }
            }
        }
        return needtypes;
    }

    public static Class getClass(Type type, int i) {
        if (type instanceof ParameterizedType) { // 处理泛型类型
            return getGenericClass((ParameterizedType) type, i);
        } else if (type instanceof TypeVariable) {
            return (Class) getClass(((TypeVariable) type).getBounds()[0], 0); // 处理泛型擦拭对象
        } else {// class本身也是type，强制转型
            return (Class) type;
        }
    }

    public static Type getType(Type type, int i) {
        if (type instanceof ParameterizedType) { // 处理泛型类型
            return getGenericType((ParameterizedType) type, i);
        } else if (type instanceof TypeVariable) {
            return getType(((TypeVariable) type).getBounds()[0], 0); // 处理泛型擦拭对象
        } else {// class本身也是type，强制转型
            return type;
        }
    }

    public static Type getParameterizedType(Type type, int i) {
        if (type instanceof ParameterizedType) { // 处理泛型类型
            Type genericType = ((ParameterizedType) type).getActualTypeArguments()[i];
            return genericType;
        } else if (type instanceof TypeVariable) {
            return getType(((TypeVariable) type).getBounds()[0], 0); // 处理泛型擦拭对象
        } else {// class本身也是type，强制转型
            return type;
        }
    }

    public static Class getGenericClass(ParameterizedType parameterizedType, int i) {
        Type genericClass = parameterizedType.getActualTypeArguments()[i];
        if (genericClass instanceof ParameterizedType) { // 处理多级泛型
            return (Class) ((ParameterizedType) genericClass).getRawType();
        } else if (genericClass instanceof GenericArrayType) { // 处理数组泛型
            return (Class) ((GenericArrayType) genericClass).getGenericComponentType();
        } else if (genericClass instanceof TypeVariable) { // 处理泛型擦拭对象
            return (Class) getClass(((TypeVariable) genericClass).getBounds()[0], 0);
        } else {
            return (Class) genericClass;
        }
    }

    public static Type getGenericType(ParameterizedType parameterizedType, int i) {
        Type genericType = parameterizedType.getActualTypeArguments()[i];
        if (genericType instanceof ParameterizedType) { // 处理多级泛型
            return ((ParameterizedType) genericType).getRawType();
        } else if (genericType instanceof GenericArrayType) { // 处理数组泛型
            return ((GenericArrayType) genericType).getGenericComponentType();
        } else if (genericType instanceof TypeVariable) { // 处理泛型擦拭对象
            return getClass(((TypeVariable) genericType).getBounds()[0], 0);
        } else {
            return genericType;
        }
    }

    /**
     * 普通类反射获取泛型方式，获取需要实际解析的类型
     *
     * @param <T>
     * @return
     */
    public static <T> Type findNeedClass(Class<T> cls) {
        //以下代码是通过泛型解析实际参数,泛型必须传
        Type genType;
        if (cls.getGenericInterfaces() == null || cls.getGenericInterfaces().length == 0) {
            genType = cls.getGenericSuperclass();
        } else {
            genType = cls.getGenericInterfaces()[0];
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        Type type = params[0];
        Type finalNeedType;
        if (params.length > 1) {//这个类似是：CacheResult<SkinTestResult> 2层
            if (!(type instanceof ParameterizedType)) throw new IllegalStateException("没有填写泛型参数");
            finalNeedType = getGenericType((ParameterizedType) type, 0);
            //Type rawType = ((ParameterizedType) type).getRawType();
        } else {//这个类似是:SkinTestResult  1层
            finalNeedType = type;
        }
        return finalNeedType;
    }

    /**
     * 普通类反射获取泛型方式，获取最顶层的类型
     */
    public static <T> Type findRawType(Class<T> cls) {
        Type genType = cls.getGenericSuperclass();
        return getGenericType((ParameterizedType) genType, 0);
    }

    public static boolean equals(Type a, Type b) {
        if (a == b) {
            return true; // Also handles (a == null && b == null).
        } else if (a instanceof Class) {
            return a.equals(b); // Class already specifies equals().
        } else if (a instanceof ParameterizedType) {
            if (!(b instanceof ParameterizedType)) return false;
            ParameterizedType pa = (ParameterizedType) a;
            ParameterizedType pb = (ParameterizedType) b;
            return pa.getRawType().equals(pb.getRawType())
                    && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());
        } else if (a instanceof GenericArrayType) {
            if (!(b instanceof GenericArrayType)) return false;
            GenericArrayType ga = (GenericArrayType) a;
            GenericArrayType gb = (GenericArrayType) b;
            return equals(ga.getGenericComponentType(), gb.getGenericComponentType());
        } else if (a instanceof WildcardType) {
            if (!(b instanceof WildcardType)) return false;
            WildcardType wa = (WildcardType) a;
            WildcardType wb = (WildcardType) b;
            return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds())
                    && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());
        } else if (a instanceof TypeVariable) {
            if (!(b instanceof TypeVariable)) return false;
            TypeVariable<?> va = (TypeVariable<?>) a;
            TypeVariable<?> vb = (TypeVariable<?>) b;
            return va.getGenericDeclaration() == vb.getGenericDeclaration()
                    && va.getName().equals(vb.getName());
        } else {
            return false; // This isn't a type we support!
        }
    }



}
