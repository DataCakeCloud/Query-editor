package com.ushareit.query.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.scheduling.annotation.Async;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;

/**
 * @author: tianxu
 * @create: 2022-06-13
 */

public interface ProbeService {
    /**
     * 探查数据
     *
     * @param uuid
     * @param user
     *
     */
    HashMap<String, Object> probe(String uuid, String user);

    /**
     * 获取探查样本
     *
     * @param uuid
     *
     */
    JSONObject getSample(String uuid);

    /**
     * 扫描数据量
     *
     * @param uuid
     * @param engine
     * @param querySql
     * @param user
     *
     */
    HashMap<String, Object> scan(String uuid, String engine, String querySql, String user) throws SQLException;
}
