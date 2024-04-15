package com.ushareit.query.utils;


import java.sql.Timestamp;

/**
 * @program: hebe
 * @description:
 * @author: wuyan
 * @create: 2020-05-09 23:12
 **/
public class TimeUtils {
    public static Timestamp longToTimestamp(Long time) {
        Timestamp newTime = null;
        if (time == null) {
            long l = System.currentTimeMillis();
            newTime = new Timestamp(l);
        }else {
            newTime = new Timestamp(time);
        }
        return newTime;
    }

    public static void main(String[] args) {
        Timestamp timestamp = new Timestamp(1589038654000L);
        System.out.println(timestamp);
    }
}
