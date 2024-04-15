package com.ushareit.query.utils;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

/**
 * 释放资源工具类
 *
 * @author wuyan
 * @date 2018/11/15
 */
@Slf4j
public class ReleaseResourceUtil {

    /**
     * 释放资源
     *
     * @param obj
     */
    public static void close(Object obj) {
        invoke(obj, "close");
    }

    /**
     * 释放资源
     *
     * @param obj
     * @param method 方法名
     */
    public static void invoke(Object obj, String method) {
        try {
            if (obj != null) {
                Class clazz = obj.getClass();
                Method close = clazz.getMethod(method);
                close.setAccessible(true);
                close.invoke(obj);
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            log.error("release resource execute failed: ", e);
            obj = null;
        }
    }
}
