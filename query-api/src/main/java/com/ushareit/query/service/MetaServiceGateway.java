package com.ushareit.query.service;

import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.Meta;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface MetaServiceGateway extends BaseService<Meta> {
    /**
     * 获取元数据database
     *
     * @param engine
     * @param name
     * @param catalog
     */
    List<String> getMetaDatabase(String engine, String name, String catalog, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException;

    /**
     * 获取元数据tables
     *
     * @param engine
     * @param name
     * @param catalog
     * @param database
     */
    List<String> getMetaTable(String engine, String name, String catalog, String database, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException;

    /**
     * 获取元数据columns
     *
     * @param engine
     * @param name
     * @param catalog
     * @param database
     * @param table
     */
    List<Map<String, String>> getMetaColumn(String engine, String name, String catalog, String database,
                                            String table, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException;

    List<Map<String, String>> getTableOwner(String engine, String name, String catalog,
                                            String database, String table, String tenantName, String region, CurrentUser currentUser) throws ParseException;
}
