package com.ushareit.query.web.utils;

import java.util.UUID;

/**
 * @author: licg
 * @create: 2020-06-30
 **/
public class UuidUtil {
    /**
     * 获取UUID
     *
     * @return
     */
    public static String getUuid32() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return uuid;
    }
}
