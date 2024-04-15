package com.ushareit.query.service;

import com.ushareit.query.bean.Meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface MetaService extends BaseService<Meta> {
    /**
     * 获取用户group
     *
     * @param name
     */
//    String getUserGroup(String name);

    /**
     * 获取元数据meta
     * @param engine
     * @param name
     *
     */
    List<String> getMetaCatalog(String engine, String name, String region);

    /**
     * 获取元数据database
     *
     * @param engine
     * @param name
     * @param catalog
     */
    List<String> getMetaDatabase(String engine, String name, String catalog, String tenantName, String region);

    /**
     * 获取元数据tables
     *
     * @param engine
     * @param name
     * @param catalog
     * @param database
     */
    List<String> getMetaTable(String engine, String name, String catalog, String database, String tenantName, String region);

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
            String table, String tenantName, String region);

    /**
     * 获取元数据meta
     * @param region
     * @param name
     *
     */
    ArrayList<Object> getMetaCatalogAndEngine(String name, String region, String tenentName);

    List<String> getClusterNodeFromK8s();

    void forwardRequest(String ip_str, String path, String userInfo);

    List<Map<String, String>> getTableOwner(String engine, String name, String catalog,
                                            String database, String table, String tenantName, String region);

    ArrayList<Object> getMetaCatalogAndEngineFromDS(String name, String region, String tenentName,
                                                    String token, String groupUuid, boolean isAdmin);
}
