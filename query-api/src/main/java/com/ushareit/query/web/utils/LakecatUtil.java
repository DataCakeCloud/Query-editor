package com.ushareit.query.web.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LakecatUtil {

    private static String tansRegion(String region, String engine) {
        if (region.equals("aws_ue1")) {
            if (engine.startsWith("hive")) {
                return "aws_us-east-1";
            }
            return "shareit_ue1";
        } else if (region.equals("aws_sg")) {
            return "shareit_sg1";
        } else if (region.equals("huawei_sg")){
            return "shareit_sg2";
        } else {
            return region;
        }
    }

    public static List<String> getMetaDatabase(String dbsUrl, String tenantName, String region, String engine) {
        String url = String.format(dbsUrl, tenantName, tansRegion(region, engine));
        List<String> databases = new ArrayList<>();
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Authorization", "qwe4677frd");
            heads.put("Request-Origion", "SwaggerBootstrapUi");
            heads.put("accept", "application/json;charset=UTF-8");
            String resInfo = CommonUtil.httpResult(url, true, null, heads, dbsUrl);
            Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            List listObjects = JSON.parseObject(data.get("objects").toString(), List.class);
            for (int i = 0; i < listObjects.size(); ++i) {
                Map dbObject = JSON.parseObject(listObjects.get(i).toString(), Map.class);
                databases.add(dbObject.get("databaseName").toString());
            }
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while get dbs from lk: %s",
                    url, CommonUtil.printStackTraceToString(e)));
        }
        return databases;
    }

    public static List<String> getMetaTable(String tablesUrl, String database, String tenantName, String region, String engine) {
        String url = String.format(tablesUrl, tenantName, tansRegion(region, engine), database);
        List<String> tables = new ArrayList<>();
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Authorization", "qwe4677frd");
            heads.put("Request-Origion", "SwaggerBootstrapUi");
            heads.put("accept", "application/json;charset=UTF-8");
            String resInfo = CommonUtil.httpResult(url, true, null, heads, database);
            Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            List listObjects = JSON.parseObject(data.get("objects").toString(), List.class);
            for (int i = 0; i < listObjects.size(); ++i) {
                Map dbObject = JSON.parseObject(listObjects.get(i).toString(), Map.class);
                tables.add(dbObject.get("tableName").toString());
            }
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while get tables from lk: %s",
                    url, CommonUtil.printStackTraceToString(e)));
        }
        return tables;
    }

    public static List<Map<String, String>> getMetaColumn(String tableUrl, String database, String table,
                                                          String tenantName, String region, String engine) {
        String url = String.format(tableUrl, tenantName, tansRegion(region, engine), database, table);
        List<Map<String, String>> columns = new ArrayList<>();
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Authorization", "qwe4677frd");
            heads.put("Request-Origion", "SwaggerBootstrapUi");
            heads.put("accept", "application/json;charset=UTF-8");
            String resInfo = CommonUtil.httpResult(url, true, null, heads, table);
            Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            Map desc = JSON.parseObject(data.get("storageDescriptor").toString(), Map.class);
            List listFields = JSON.parseObject(desc.get("columns").toString(), List.class);
            for (int i = 0; i < listFields.size(); ++i) {
                Map field = JSON.parseObject(listFields.get(i).toString(), Map.class);
                Map<String, String> column = new HashMap<>();
                column.put("columnName", field.get("columnName").toString());
                column.put("columnType", field.get("colType").toString());
                column.put("columnComment", field.get("comment").toString());
                columns.add(column);
            }
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while get table from lk: %s",
                    url, CommonUtil.printStackTraceToString(e)));
        }
        return columns;
    }

    public static ArrayList getTableOwner(String tableUrl, String database, String table,
                                          String tenantName, String region, String engine) {
        String url = String.format(tableUrl, tenantName, tansRegion(region, engine), database, table);
        ArrayList owner = new ArrayList<>();
        try {
            Map<String, String> heads = new HashMap<>();
            heads.put("Authorization", "qwe4677frd");
            heads.put("Request-Origion", "SwaggerBootstrapUi");
            heads.put("accept", "application/json;charset=UTF-8");
            String resInfo = CommonUtil.httpResult(url, true, null, heads, table);
            Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            owner.add(data.get("owner").toString());
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while get table from lk: %s",
                    url, CommonUtil.printStackTraceToString(e)));
        }
        return owner;
    }
}
