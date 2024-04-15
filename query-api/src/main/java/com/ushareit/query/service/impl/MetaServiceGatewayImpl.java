package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.configuration.GatewayConfig;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.MetaService;
import com.ushareit.query.service.MetaServiceGateway;
import com.ushareit.query.web.utils.CommonUtil;
import com.ushareit.query.web.utils.GatewayUtil;
import com.ushareit.query.web.utils.LakecatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
public class MetaServiceGatewayImpl extends AbstractBaseServiceImpl<Meta> implements MetaServiceGateway {
    @Resource
    private MetaMapper metaMapper;

    @Resource
    private AccountMapper accountMapper;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${lakecat.url}")
    private String lkUrl;

    @Value("${de.gateway}")
    private String deUrl;

    @Autowired
    private GatewayConfig gatewayConfig;

    @Override
    public CrudMapper<Meta> getBaseMapper() { return metaMapper; }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#name")
    public List<String> getMetaDatabase(String engine, String name, String catalog, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException {
        log.info(String.format("%s start to get database in engine %s and catalog %s without cache", name, engine, catalog));
        if (catalog.equals("iceberg")) {
            catalog = "hive";
        }

        if (catalog.equalsIgnoreCase("hive") || catalog.equalsIgnoreCase("trino")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String dbsUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/list?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaDatabase(dbsUrl, tenantName, region, engine);
        }

        String sql =  String.format("show databases");
        Properties properties = new Properties();
        ArrayList databases = new ArrayList<>();
        try {
            GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
            GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, catalog, "", deUrl,
                token, "", "", accountMapper);

            String url = gwProperties.getUrlWithParams();
            databases = CommonUtil.getQueryResultsFromGW(properties, url, sql, "");
        }catch(Exception e){
             e.printStackTrace();
        }
        return databases;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-tables'")
    public List<String> getMetaTable(String engine, String name, String catalog, String database, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException {
        log.info(String.format("%s start to get table in engine %s, catalog %s and database %s without cache", name, engine, catalog, database));
        if (catalog.equals("iceberg")) {
            catalog = "hive";
        }
        if (catalog.equalsIgnoreCase("hive") || catalog.equalsIgnoreCase("trino")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tablesUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaTable(tablesUrl, database, tenantName, region, engine);
        }

        ArrayList tables = new ArrayList<>();
        Properties properties = new Properties();
        try {
            GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
            GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, catalog, database, deUrl,
                token, "", "", accountMapper);

            String url = gwProperties.getUrlWithParams();
            String sql = String.format("show tables");
            System.out.println(sql);

            tables = CommonUtil.getQueryResultsFromGW(properties, url, sql, "");
        }catch(Exception e){
            e.printStackTrace();
        }

        return tables;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-'+#table+'-columns'")
    public List<Map<String, String>> getMetaColumn(String engine, String name, String catalog, String database,
                                            String table, String tenantName, String region, CurrentUser currentUser, String token) throws ParseException {
        log.info(String.format("%s start to get column in engine %s, catalog %s, database %s and table %s without cache", name, engine, catalog, database, table));
        if (catalog.equals("iceberg")) {
            catalog = "hive";
        }
        if (catalog.equalsIgnoreCase("hive") || catalog.equalsIgnoreCase("trino")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tableUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables/%s?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaColumn(tableUrl, database, table, tenantName, region, engine);
        }

        ArrayList columns = new ArrayList<>();
        Properties properties = new Properties();
        try {
            GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
            GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, catalog, database, deUrl,
                token, "", "", accountMapper);

            String url = gwProperties.getUrlWithParams();
            String sql = String.format("desc %s", table);

            System.out.println(sql);
            columns = CommonUtil.getQueryResultsFromGW(properties, url, sql, "");
        }catch(Exception e){
            e.printStackTrace();
        }

        return columns;
    }

    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-'+#table+'-tableOwner'")
    public List<Map<String, String>> getTableOwner(String engine, String name, String catalog,
                                            String database, String table, String tenantName, String region, CurrentUser currentUser) throws ParseException {
        log.info(String.format("%s start to get table owner in engine %s, catalog %s, database %s and table %s without cache", name, engine, catalog, database, table));
        if (catalog.equalsIgnoreCase("hive")
                || catalog.equalsIgnoreCase("iceberg")
                || catalog.equalsIgnoreCase("trino")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tableUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables/%s?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getTableOwner(tableUrl, database, table, tenantName, region, engine);
        }

        Properties properties = new Properties();
        GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
        GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, catalog, database, deUrl,
          "", "", "", accountMapper);

        String url = gwProperties.getUrlWithParams();
        String sql =  String.format("desc extended %s.%s", database, table);
        System.out.println(sql);
        ArrayList tableOwners = CommonUtil.getQueryResultsFromGW(properties, url, sql, "");
        return tableOwners;
    }
}
