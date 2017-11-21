package com.whiker.learn.common.dynamiccompile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author whiker@163.com create on 16-5-7.
 */
public class ClassUtil {

    public static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建类的实例
     *
     * @param srcClass 类
     * @param params   构造器参数
     * @return 实例
     */
    public static Object newInstance(Class<?> srcClass, Object... params) throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        Constructor constructor = srcClass.getConstructor(paramTypes);
        return constructor.newInstance(params);
    }
}
