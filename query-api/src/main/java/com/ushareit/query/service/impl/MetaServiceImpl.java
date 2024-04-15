package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.configuration.OlapConfig;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.mapper.RegionMapper;
import com.ushareit.query.mapper.UserMapper;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.MetaService;
import com.ushareit.query.web.utils.CommonUtil;
import com.ushareit.query.web.utils.LakecatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Slf4j
@Service
public class MetaServiceImpl extends AbstractBaseServiceImpl<Meta> implements MetaService {

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private AccountMapper accountMapper;
    
    @Resource
    private RegionMapper regionMapper;
    
    @Resource
    private UserMapper userMapper;

    @Override
    public CrudMapper<Meta> getBaseMapper() { return metaMapper; }

    @Value("${olap.url.aws_ue1}")
    private String awsUrl;

    @Value("${olap.url.aws_sg}")
    private String awsSGUrl;

    @Value("${olap.url.huawei_sg}")
    private String huaweiUrl;

    @Autowired
    private OlapConfig olapUrl;

    @Value("${lakecat.url}")
    private String lkUrl;

    @Value("${de.gateway}")
    private String deUrl;

    @Value("${server.port}")
    private int serverPort;

    @Value("${k8s.token_file}")
    private String k8sTokenFile;

    @Value("${k8s.host_env}")
    private String k8sHostEnv;

    @Value("${k8s.port_env}")
    private String k8sPortEnv;

    @Value("${k8s.namespace}")
    private String k8sNamespace;

    @Value("${k8s.endpoint}")
    private String k8sEndpoint;

//    @Override
//    @Cacheable(cacheNames = {"metadata"}, key = "#name")
//    public String getUserGroup(String name) {
//        log.info(String.format("%s start to get group without cache", name));
//        List<Account> account = accountMapper.listAll();
//        JSONObject groupAccount = CommonUtil.getUserGroup(name, account, adminUsername, adminPassword);
//        String group = groupAccount.getString("group");
//        return group;
//    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#engine+'-'+#name")
    public List<String> getMetaCatalog(String engine, String name, String region) {
        log.info(String.format("%s start to get catalog in engine %s without cache", name, engine));
        ArrayList catalogs = new ArrayList<>();
        List<Account> account = accountMapper.listAll();
        if (null != region && region.trim().length() > 0) {
            catalogs.add("hive");
            catalogs.add("iceberg");
            return catalogs;
        }

        JSONObject connectInfo = CommonUtil.getUsernameAndPassword(account, "BDP", engine, awsSGUrl, awsUrl, huaweiUrl);
        String username = connectInfo.getString("username");
        String password = connectInfo.getString("password");
        String url = connectInfo.getString("url");
        String provider = connectInfo.getString("provider");

        String sql = "";

        if (engine.equals("presto_aws_sg") || engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("ares_ue1") || engine.equals("smart_aws_sg")) {

            catalogs.add("hive");

        } else {

//            sql = String.format("show catalogs");
//            System.out.println(sql);
            catalogs.add("hive");
            catalogs.add("iceberg");

//            catalogs = CommonUtil.getQueryResults(username, password, url, sql, provider);
        }
        return catalogs;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#name")
    public List<String> getMetaDatabase(String engine, String name, String catalog, String tenantName, String region) {
        log.info(String.format("%s start to get database in engine %s and catalog %s without cache", name, engine, catalog));
        if (!region.equals("aws_sg") && catalog.equals("iceberg")) {
            catalog = "hive";
        }

        if (!region.equals("aws_sg") && catalog.equals("hive")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String dbsUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/list?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaDatabase(dbsUrl, tenantName, region, engine);
        }

        ArrayList databases = new ArrayList<>();

        if (engine.startsWith("ch") || engine.startsWith("mysql") || engine.startsWith("tidb")) {
            Meta engineInfo = metaMapper.listByKey(engine);

            String engineDatabase = engineInfo.getEngineDatabase();
            databases.add(engineDatabase);

        } else if (engine.startsWith("presto") || engine.startsWith("spark") || engine.startsWith("ares") || engine.startsWith("smart")) {
            List<Account> account = accountMapper.listAll();

            JSONObject connectInfo = CommonUtil.getUsernameAndPassword(account, "BDP", engine, awsSGUrl, awsUrl, huaweiUrl);
            String username = connectInfo.getString("username");
            String password = connectInfo.getString("password");
            String url = connectInfo.getString("url");
            String provider = connectInfo.getString("provider");

            String sql = String.format("--conf bdp-query-tenancy=%s\n", tenantName);

            if (engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("smart_aws_sg")) {

                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=ares_ap1\n show schemas", name);
            } else {
                sql += String.format("--conf bdp-query-user=%s\nshow schemas from %s", name, catalog);
            }
            url = olapUrl.getUrl().get(region);
            System.out.println(sql);
            boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
            if (!use_ssl) {
                password = "";
            }

            databases = CommonUtil.getQueryResults(username, password, url, sql, provider);
        }
        return databases;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-tables'")
    public List<String> getMetaTable(String engine, String name, String catalog, String database, String tenantName, String region) {
        log.info(String.format("%s start to get table in engine %s, catalog %s and database %s without cache", name, engine, catalog, database));
        if (!region.equals("aws_sg") && catalog.equals("iceberg")) {
            catalog = "hive";
        }
        if (!region.equals("aws_sg") && catalog.equals("hive")) {String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tablesUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaTable(tablesUrl, database, tenantName, region, engine);
        }

        List<Account> account = accountMapper.listAll();

        JSONObject connectInfo = CommonUtil.getUsernameAndPassword(account, "BDP", engine, awsSGUrl, awsUrl, huaweiUrl);
        String username = connectInfo.getString("username");
        String password = connectInfo.getString("password");

        String url = "";
        String provider = "aws";
        String sql = String.format("--conf bdp-query-tenancy=%s\n", tenantName);


        if (engine.equals("presto_aws") || engine.equals("presto_aws_sg") || engine.equals("presto_huawei")
                || engine.equals("spark-submit-sql-3_aws_us-east-1") || engine.equals("spark-submit-sql-3_huawei_ap-southeast-3")
                || engine.equals("ares_ue1") || engine.equals("smart_aws") || engine.equals("smart_huawei")) {

            url = connectInfo.getString("url");
            provider = connectInfo.getString("provider");
            sql += String.format("--conf bdp-query-user=%s\nshow tables from %s.%s", name, catalog, database);

        } else if (engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("smart_aws_sg")) {
            url = connectInfo.getString("url");
            provider = connectInfo.getString("provider");
            sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=ares_ap1\n show tables from %s", name, database);
        } else if(!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
            url = olapUrl.getUrl().get(region);
        } else {
            url = awsUrl;
            sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=%s\n show tables from `%s`", name, engine, database);
        }

        System.out.println(sql);boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
        if (!use_ssl) {
            password = "";
        }

        ArrayList tables = CommonUtil.getQueryResults(username, password, url, sql, provider);

        return tables;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-'+#table+'-columns'")
    public List<Map<String, String>> getMetaColumn(String engine, String name, String catalog,
            String database, String table, String tenantName, String region) {
        log.info(String.format("%s start to get column in engine %s, catalog %s, database %s and table %s without cache", name, engine, catalog, database, table));
        if (!region.equals("aws_sg") && catalog.equals("iceberg")) {
            catalog = "hive";
        }
        if (!region.equals("aws_sg") && catalog.equals("hive")) {
            String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tableUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables/%s?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getMetaColumn(tableUrl, database, table, tenantName, region, engine);
        }

        List<Account> account = accountMapper.listAll();

        JSONObject connectInfo = CommonUtil.getUsernameAndPassword(account, "BDP", engine, awsSGUrl, awsUrl, huaweiUrl);
        String username = connectInfo.getString("username");
        String password = connectInfo.getString("password");

        String url = "";
        String provider = "aws";
        String sql = String.format("--conf bdp-query-tenancy=%s\n", tenantName);

        if (engine.startsWith("presto") || engine.startsWith("spark") || engine.startsWith("ares") || engine.startsWith("smart")) {

            url = connectInfo.getString("url");
            provider = connectInfo.getString("provider");

            if (engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("smart_aws_sg")) {

                provider = "aws_sg";
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=ares_ap1\n show columns from %s.%s", name, database, table);

            } else {

                sql += String.format("--conf bdp-query-user=%s\nshow columns from %s.%s.%s", name, catalog, database, table);

            }

        } else if(!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
            url = olapUrl.getUrl().get(region);
        } else {
            url = awsUrl;
            if (engine.startsWith("ch")) {
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=%s\n describe table %s.%s", name, engine, database, table);
            } else {
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=%s\n show columns from %s.%s", name, engine, database, table);
            }

        }

        System.out.println(sql);boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
        if (!use_ssl) {
            password = "";
        }

        ArrayList columns = CommonUtil.getQueryResults(username, password, url, sql, provider);

        return columns;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#region+'-'+#name")
    public ArrayList<Object> getMetaCatalogAndEngine(String name, String region, String tenantName) {
    	log.info(String.format("%s start to get catalog and engines in region %s without cache", name, region));

        ArrayList<Object> catalogAndEngines = new ArrayList<>();
    	ArrayList<Object> hiveEngine = new ArrayList<>();
    	ArrayList<Object> iceBergEngine = new ArrayList<>();
    	List<Meta> userEngineSmartList = metaMapper.listForSmart();
        for (int i = 0; i < userEngineSmartList.size(); i++) {
            Meta engineSmart = userEngineSmartList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equals(engineSmart.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineSmart = new HashMap<>();
            userEngineSmart.put("label", engineSmart.getEngineName());
            userEngineSmart.put("value", engineSmart.getEngineKey());
            userEngineSmart.put("database", "");
            HashMap<String, String> userEngineSmart1 = new HashMap<>();
            userEngineSmart1.putAll(userEngineSmart);
            hiveEngine.add(userEngineSmart);
            iceBergEngine.add(userEngineSmart1);
        }

        List<Meta> userEnginePrestoList = metaMapper.listForPresto();
        for (int i = 0; i < userEnginePrestoList.size(); i++) {
            Meta enginePresto = userEnginePrestoList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(enginePresto.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEnginePresto = new HashMap<>();
            userEnginePresto.put("label", enginePresto.getEngineName());
            userEnginePresto.put("value", enginePresto.getEngineKey());
            userEnginePresto.put("database", "");
            HashMap<String, String> userEnginePresto1 = new HashMap<>();
            userEnginePresto1.putAll(userEnginePresto);
            hiveEngine.add(userEnginePresto);
            iceBergEngine.add(userEnginePresto1);
        }
        if (hiveEngine.isEmpty()) {
            HashMap<String, String> userEnginePresto = new HashMap<>();
            userEnginePresto.put("label", "Ares");
            userEnginePresto.put("value", "presto_" + region);
            userEnginePresto.put("database", "");
            HashMap<String, String> userEnginePresto1 = new HashMap<>();
            userEnginePresto1.putAll(userEnginePresto);
            hiveEngine.add(userEnginePresto);
            iceBergEngine.add(userEnginePresto1);
        }

        List<Meta> userEngineSparkList = metaMapper.listForSpark();
        for (int i = 0; i < userEngineSparkList.size(); i++) {
            Meta engineSpark = userEngineSparkList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(engineSpark.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineSpark = new HashMap<>();
            userEngineSpark.put("label", engineSpark.getEngineName());
            userEngineSpark.put("value", engineSpark.getEngineKey());
            userEngineSpark.put("database", "");
            HashMap<String, String> userEngineSpark1 = new HashMap<>();
            userEngineSpark1.putAll(userEngineSpark);
            hiveEngine.add(userEngineSpark);
            iceBergEngine.add(userEngineSpark1);
        }

        /*List<Meta> userEngineAresList = metaMapper.listForAres();
        for (int i = 0; i < userEngineAresList.size(); i++) {
            Meta engineAres = userEngineAresList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(engineAres.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineAres = new HashMap<>();
            userEngineAres.put("label", engineAres.getEngineName());
            userEngineAres.put("value", engineAres.getEngineKey());
            userEngineAres.put("database", "");
            HashMap<String, String> userEngineAres1 = new HashMap<>();
            userEngineAres1.putAll(userEngineAres);
            hiveEngine.add(userEngineAres);
            iceBergEngine.add(userEngineAres1);
        }*/

        if (region.equals("aws_sg")) {
            HashMap<String, Object> hiveEngins = new HashMap<>();
            hiveEngins.put("catalog", "hive");
            hiveEngins.put("engines", hiveEngine);
            catalogAndEngines.add(hiveEngins);
        }
        HashMap<String, Object> iceBergEngins = new HashMap<>();
        iceBergEngins.put("catalog", "iceberg");
        iceBergEngins.put("engines", iceBergEngine);
        catalogAndEngines.add(iceBergEngins);

        String otherEngines = userMapper.selectEngineByName(name);
        JSONArray engines = JSONArray.parseArray(otherEngines);
        if (engines != null) {
            for(int i = 0; i < engines.size(); i++) {
                HashMap<String, String> userEngineOtherMap = new HashMap<>();
                JSONObject engine = engines.getJSONObject(i);
                String engineValue = engine.getString("value");
                String engineLabel = engine.getString("label");
                String engineDatabase = "";
                if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(metaMapper.listRegionByKey(engineValue))) {
                	continue;
                }

                if (!engineValue.startsWith("presto") && !engineValue.startsWith("spark") && !engineValue.startsWith("ares")) {
                    engineDatabase = metaMapper.listByKey(engineValue).getEngineDatabase();
                }

                userEngineOtherMap.put("label", engineLabel);
                userEngineOtherMap.put("value", engineValue);
                userEngineOtherMap.put("database", engineDatabase);
                ArrayList userEngineOther = new ArrayList<>();
                userEngineOther.add(userEngineOtherMap);
                HashMap<String, Object> otherEngins = new HashMap<>();
                otherEngins.put("catalog", engineValue);
                otherEngins.put("engines", userEngineOther);
                catalogAndEngines.add(otherEngins);
            }
        }
        
        return catalogAndEngines;
    }

    @Override
    public List<String> getClusterNodeFromK8s() {
        List<String> ret = new ArrayList<String>();
        try {
            String k8sToken = Files.readAllLines(Paths.get(k8sTokenFile)).get(0);
            String k8sHost = System.getenv(k8sHostEnv);
            String k8sPort = System.getenv(k8sPortEnv);
            log.info(String.format("k8s info: token {%s}, host {%s}, port {%s}",
                    k8sToken, k8sHost, k8sPort));
            String url = String.format("https://%s:%s/api/v1/namespaces/%s/endpoints/%s",
                    k8sHost, k8sPort, k8sNamespace, k8sEndpoint);
            Map<String, String> heads = new HashMap<>();
            heads.put("Authorization", "Bearer " + k8sToken);
            String resInfo = CommonUtil.httpsReuslt(url, true, null, heads);
            Map content = JSON.parseObject(resInfo, Map.class);
            List subsets = JSON.parseObject(content.get("subsets").toString(), List.class);
            for (int i = 0; i < subsets.size(); ++i) {
                Map subset = JSON.parseObject(subsets.get(i).toString(), Map.class);
                List addresses = JSON.parseObject(subset.get("addresses").toString(), List.class);
                for (int j = 0; j < addresses.size(); ++j) {
                    Map address = JSON.parseObject(addresses.get(j).toString(), Map.class);
                    String ip = address.get("ip").toString();
                    ret.add(ip);
                    log.info(String.format("k8s info: find ip {%s}", ip));
                }
            }
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while get cluster info: %s",
                    CommonUtil.printStackTraceToString(e)));
        }
        return ret;
    }

    @Override
    public void forwardRequest(String ip_str, String path, String userInfo) {
        try {
            Map<String, String> heads = new HashMap<String, String>();
            heads.put(CommonConstant.CURRENT_LOGIN_USER, userInfo);
            CommonUtil.httpResult("http://" + ip_str + ":" + String.valueOf(serverPort) + path,
                true, null, heads, userInfo);
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while forward request: %s",
                    CommonUtil.printStackTraceToString(e)));
        }
    }

    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#engine+'-'+#catalog+'-'+#database+'-'+#table+'-tableOwner'")
    public List<Map<String, String>> getTableOwner(String engine, String name, String catalog,
                                                   String database, String table, String tenantName, String region) {
        log.info(String.format("%s start to get table owner in engine %s, catalog %s, database %s and table %s without cache", name, engine, catalog, database, table));
        if (!region.equals("aws_sg") && (catalog.equals("hive") || catalog.equals("iceberg"))) {String seg = "";
            if ('/' != lkUrl.charAt(lkUrl.length() - 1)) {
                seg = "/";
            }
            String tableUrl = lkUrl + seg + "v1/%s/catalogs/%s/databases/%s/tables/%s?filter=&includeDrop=false&maxResults=1000";
            return LakecatUtil.getTableOwner(tableUrl, database, table, tenantName, region, engine);
        }

        List<Account> account = accountMapper.listAll();

        JSONObject connectInfo = CommonUtil.getUsernameAndPassword(account, "BDP", engine, awsSGUrl, awsUrl, huaweiUrl);
        String username = connectInfo.getString("username");
        String password = connectInfo.getString("password");

        String url = "";
        String provider = "aws";
        String sql = String.format("--conf bdp-query-tenancy=%s\n", tenantName);

        if (engine.startsWith("presto") || engine.startsWith("spark") || engine.startsWith("ares") || engine.startsWith("smart")) {

            url = connectInfo.getString("url");
            provider = connectInfo.getString("provider");
            if (engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("smart_aws_sg")) {
                provider = "aws_sg";
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=ares_ap1\n desc extended %s.%s", name, database, table);
            } else {
                if (0 == catalog.compareToIgnoreCase("hive")) {
                    sql += String.format("--conf bdp-query-user=%s\ndesc extended %s.%s", name, database, table);
                } else {
                    sql += String.format("--conf bdp-query-user=%s\ndesc extended %s.%s.%s", name, catalog, database, table);
                }

            }

        } else {
            url = awsUrl;
            if (engine.startsWith("ch")) {
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=%s\n desc extended %s.%s", name, engine, database, table);
            } else {
                sql += String.format("--conf bdp-query-user=%s\n--conf bdp-query-engine=%s\n desc extended %s.%s", name, engine, database, table);
            }
        }

        url = olapUrl.getUrl().get(region);
        System.out.println(sql);boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
        if (!use_ssl) {
            password = "";
        }
        ArrayList tableOwners = CommonUtil.getQueryResults(username, password, url, sql, provider);
        return tableOwners;
    }

    @Override
    @Cacheable(cacheNames = {"metadata"}, key = "#tenantName+'-'+#region+'-'+#name+'-'+#groupUuid")
    public ArrayList<Object> getMetaCatalogAndEngineFromDS(String name, String region, String tenantName,
                                                           String token, String groupUuid, boolean isAdmin) {
        log.info(String.format("%s start to get catalog and engines from data source in region %s without cache", name, region));

        ArrayList<Object> catalogAndEngines = new ArrayList<>();
        ArrayList<Object> hiveEngine = new ArrayList<>();
        ArrayList<Object> iceBergEngine = new ArrayList<>();
        List<Meta> userEngineSmartList = metaMapper.listForSmart();
        for (int i = 0; i < userEngineSmartList.size(); i++) {
            Meta engineSmart = userEngineSmartList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equals(engineSmart.getRegion())) {
                continue;
            }
            HashMap<String, String> userEngineSmart = new HashMap<>();
            userEngineSmart.put("label", engineSmart.getEngineName());
            userEngineSmart.put("value", engineSmart.getEngineKey());
            userEngineSmart.put("database", "");
            HashMap<String, String> userEngineSmart1 = new HashMap<>();
            userEngineSmart1.putAll(userEngineSmart);
            hiveEngine.add(userEngineSmart);
            iceBergEngine.add(userEngineSmart1);
        }

        List<Meta> userEnginePrestoList = metaMapper.listForPresto();
        for (int i = 0; i < userEnginePrestoList.size(); i++) {
            Meta enginePresto = userEnginePrestoList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(enginePresto.getRegion())) {
                continue;
            }
            HashMap<String, String> userEnginePresto = new HashMap<>();
            userEnginePresto.put("label", enginePresto.getEngineName());
            userEnginePresto.put("value", enginePresto.getEngineKey());
            userEnginePresto.put("database", "");
            HashMap<String, String> userEnginePresto1 = new HashMap<>();
            userEnginePresto1.putAll(userEnginePresto);
            hiveEngine.add(userEnginePresto);
            iceBergEngine.add(userEnginePresto1);
        }
        if (hiveEngine.isEmpty()) {
            HashMap<String, String> userEnginePresto = new HashMap<>();
            userEnginePresto.put("label", "Ares");
            userEnginePresto.put("value", "presto_" + region);
            userEnginePresto.put("database", "");
            HashMap<String, String> userEnginePresto1 = new HashMap<>();
            userEnginePresto1.putAll(userEnginePresto);
            hiveEngine.add(userEnginePresto);
            iceBergEngine.add(userEnginePresto1);
        }

        List<Meta> userEngineSparkList = metaMapper.listForSpark();
        for (int i = 0; i < userEngineSparkList.size(); i++) {
            Meta engineSpark = userEngineSparkList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(engineSpark.getRegion())) {
                continue;
            }
            HashMap<String, String> userEngineSpark = new HashMap<>();
            userEngineSpark.put("label", engineSpark.getEngineName());
            userEngineSpark.put("value", engineSpark.getEngineKey());
            userEngineSpark.put("database", "");
            HashMap<String, String> userEngineSpark1 = new HashMap<>();
            userEngineSpark1.putAll(userEngineSpark);
            hiveEngine.add(userEngineSpark);
            iceBergEngine.add(userEngineSpark1);
        }
        if (1 == hiveEngine.size()) {
            HashMap<String, String> userEngineSpark = new HashMap<>();
            userEngineSpark.put("label", "Spark-SQL");
            userEngineSpark.put("value", "spark_" + region);
            userEngineSpark.put("database", "");
            HashMap<String, String> userEngineSpark1 = new HashMap<>();
            userEngineSpark1.putAll(userEngineSpark);
            hiveEngine.add(userEngineSpark);
            iceBergEngine.add(userEngineSpark1);
        }

        HashMap<String, Object> iceBergEngins = new HashMap<>();
        iceBergEngins.put("catalog", "iceberg");
        iceBergEngins.put("engines", iceBergEngine);
        //catalogAndEngines.add(iceBergEngins);

        {
            ArrayList<Object> hiveNineEngine = new ArrayList<>();
            HashMap<String, Object> hiveNineEngins = new HashMap<>();

            HashMap<String, String> hiveNine = new HashMap<>();
            hiveNine.put("label", "Hive");
            hiveNine.put("value", "hive_" + region);
            hiveNine.put("database", "");
            hiveNineEngine.add(hiveNine);

            HashMap<String, String> trinoNine = new HashMap<>();
            trinoNine.put("label", "Trino");
            trinoNine.put("value", "presto_" + region);
            trinoNine.put("database", "");
            hiveNineEngine.add(trinoNine);

            hiveNineEngins.put("catalog", "hive");
            hiveNineEngins.put("engines", hiveNineEngine);
            catalogAndEngines.add(hiveNineEngins);
        }

        /*List<String> dsUuids;
        if (isAdmin) {
            dsUuids = getDataSourceNameForAdimn(name, region, tenantName, token);
        } else {
            dsUuids = getDataSourceName(name, region, tenantName, token, groupUuid);
        }
        Map<String, String> region_convert = new HashMap<>();
        region_convert.put("ue1", "aws_ue1");
        region_convert.put("sg1", "aws_sg");
        region_convert.put("sg2", "huawei_sg");
        String url = deUrl + "/actor/sources/all";
        Map<String, String> heads = new HashMap<>();
        heads.put("Authentication", token);
        heads.put("Cache-Control", "no-cache");
        heads.put("accept", "application/json;charset=UTF-8");
        String resInfo = CommonUtil.httpResult(url, true, null, heads);
        Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
        List listObjects = JSON.parseObject(content.get("data").toString(), List.class);
        for (int i = 0; i < listObjects.size(); ++i) {
            Map dbObject = JSON.parseObject(listObjects.get(i).toString(), Map.class);
            String region_ds = "";
            if (null != dbObject.get("region")) {
                region_ds = dbObject.get("region").toString();
            }
            if (region_convert.containsKey(region_ds)) {
                region_ds = region_convert.get(region_ds);
            }
            if (!region_ds.equals(region)) {
                continue;
            }
            String uuid = "";
            if (null != dbObject.get("uuid")) {
                uuid = dbObject.get("uuid").toString();
            }
            if (-1 == dsUuids.indexOf(uuid)) {
                continue;
            }

            String engineValue = dbObject.get("name").toString();
            String engineLabel = engineValue;
            Map configObject = JSON.parseObject(dbObject.get("connectionConfiguration").toString(), Map.class);
            String engineDatabase = "";
            if (null != configObject.get("database")) {
                engineDatabase = configObject.get("database").toString();
            }
            Map<String, String> userEngineOtherMap = new HashMap<>();
            userEngineOtherMap.put("label", engineLabel);
            userEngineOtherMap.put("value", engineValue);
            userEngineOtherMap.put("database", engineDatabase);
            ArrayList userEngineOther = new ArrayList<>();
            userEngineOther.add(userEngineOtherMap);
            HashMap<String, Object> otherEngins = new HashMap<>();
            otherEngins.put("catalog", engineValue);
            otherEngins.put("engines", userEngineOther);
            catalogAndEngines.add(otherEngins);
        }*/

        return catalogAndEngines;
    }

    private List<String> getDataSourceName(String name, String region, String tenantName,
                                           String token, String groupUuid) {
        Map<String, String> region_convert = new HashMap<>();
        region_convert.put("aws_ue1", "ue1");
        region_convert.put("aws_sg", "sg1");
        region_convert.put("huawei_sg", "sg2");
        if (region_convert.containsKey(region)) {
            region = region_convert.get(region);
        }
        String url_path = String.format("v1/%s/roles?roleName=%s",
                tenantName, groupUuid);

        List<String> res = new ArrayList<>();
        String url = lkUrl + url_path;
        Map<String, String> heads = new HashMap<>();
        heads.put("Authorization", token);
        heads.put("Request-Origin", "SwaggerBootstrapUi");
        heads.put("accept", "application/json;charset=UTF-8");
        String resInfo = CommonUtil.httpResult(url, true, null, heads, name);
        Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
        Map mapData = JSON.parseObject(content.get("data").toString(), Map.class);
        List listRolePrivileges = JSON.parseObject(mapData.get("rolePrivileges").toString(), List.class);
        for (int i = 0; i < listRolePrivileges.size(); ++i) {
            Map mapRole = JSON.parseObject(listRolePrivileges.get(i).toString(), Map.class);
            if (mapRole.get("grantedOn").toString().equalsIgnoreCase("CATALOG")
                    && -1 != mapRole.get("privilege").toString().toUpperCase().indexOf("CATALOG")
                    && !res.contains(mapRole.get("name").toString())) {
                res.add(mapRole.get("name").toString());
            }
        }

        return res;
    }

    private List<String> getDataSourceNameForAdimn(String name, String region,
                                                   String tenantName, String token) {
        Map<String, String> region_convert = new HashMap<>();
        region_convert.put("aws_ue1", "ue1");
        region_convert.put("aws_sg", "sg1");
        region_convert.put("huawei_sg", "sg2");
        if (region_convert.containsKey(region)) {
            region = region_convert.get(region);
        }
        String filter = String.format("{\"description\":\"%s\"}", region);
        String url_path = String.format("v1/%s/roles/showPermObjectsByUser?filter=%s&objectType=CATALOG&userId=%s",
                tenantName, URLEncoder.encode(filter), name);

        List<String> res = new ArrayList<>();
        String url = lkUrl + url_path;
        Map<String, String> heads = new HashMap<>();
        heads.put("Authorization", token);
        heads.put("Request-Origin", "no-SwaggerBootstrapUi");
        heads.put("accept", "application/json;charset=UTF-8");
        String resInfo = CommonUtil.httpResult(url, true, null, heads, name);
        Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
        Map mapData = JSON.parseObject(content.get("data").toString(), Map.class);
        List listObjects = JSON.parseObject(mapData.get("objects").toString(), List.class);
        for (int i = 0; i < listObjects.size(); ++i) {
            res.add(listObjects.get(i).toString());
        }

        return res;
    }

}
