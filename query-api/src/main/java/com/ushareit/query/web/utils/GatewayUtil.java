package com.ushareit.query.web.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.configuration.GatewayConfig;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.mapper.AccountMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;

@Data
@Slf4j
public class GatewayUtil {

    @Data
    public static class GWProperties {
        Map<String, Object> propertys;
        String hive_propertys;
        CurrentUser currentUser;
        String gw_url;
        String sql;
        String batchType;
        String database;
        List<String> sqlList = new ArrayList<>();

        public GWProperties(CurrentUser cUser, String url, String sqlOrigin) {
            propertys = new HashMap<>();
            currentUser = cUser;
            gw_url = url;
            sql = sqlOrigin;
        }

        public Object setProperty(String key, Object value) {
            return propertys.put(key, value);
        }

        public void setBatchType(String batchType) {
            batchType = batchType.toLowerCase();
            this.batchType = batchType;
            sql = sql.trim();
            StringBuilder hive_pro = new StringBuilder(sql.length());
            if (batchType.equals("hive")) {
                String[] sql_arr = sql.split("\n");
                StringBuilder real_sql = new StringBuilder(sql.length());
                for (String s : sql_arr) {
                    s = s.trim();
                    if (s.isEmpty()) {
                        continue;
                    }
                    if (s.toLowerCase().startsWith("set ")) {
                        String[] conf_kv = s.substring(4).split("=");
                        if (conf_kv.length == 2) {
                            hive_pro.append("hiveconf:")
                                    .append(conf_kv[0].trim())
                                    .append("=")
                                    .append(conf_kv[1].replace(";", "").trim())
                                    .append(",");
                        }
                    } else if (s.toLowerCase().startsWith("--conf ")) {
                        String[] conf_kv = s.substring(7).split("=");
                        if (conf_kv.length == 2) {
                            propertys.put(conf_kv[0].trim(), conf_kv[1].trim());
                        }
                    } else {
                        if (!s.contains(";")) {
                            real_sql.append(s).append("\n");
                        } else {
                            String[] split = s.split(";");
                            real_sql.append(split[0]);
                            sqlList.add(real_sql.toString());
                            real_sql.delete(0, sql.length());
                            if (split.length > 1) {
                                real_sql.append(split[1]);
                            }
                        }
                    }
                }
                if (real_sql.length() > 0 && (sqlList.isEmpty() || !real_sql.toString().equals(sqlList.get(sqlList.size() - 1)))) {
                    sqlList.add(real_sql.toString());
                }
            } else if (batchType.equals("trino") || batchType.equals("presto")) {
                String[] sql_arr = sql.split("\n");
                StringBuilder real_sql = new StringBuilder(sql.length());
                for (String s : sql_arr) {
                    s = s.trim();
                    if (s.isEmpty()) {
                        continue;
                    }

                    if (s.toLowerCase().startsWith(CommonConstant.SQL_SET)) {
                        s = s.substring(4).trim();
                        if (s.toUpperCase().startsWith(CommonConstant.TRINO_SESSION)) {
                            s = s.substring(7).trim();
                        }
                        String[] conf_kv = s.split("=");
                        if (conf_kv.length == 2) {
                            hive_pro.append(conf_kv[0].trim())
                                    .append("=")
                                    .append(conf_kv[1].replace(";", "").trim())
                                    .append(",");
                        }
                    } else if (s.startsWith("--conf ")) {
                        String[] conf_kv = s.substring(7).split("=");
                        if (conf_kv.length == 2) {
                            propertys.put(conf_kv[0].trim(), conf_kv[1].trim());
                        }
                    } else {
                        if (!s.contains(";")) {
                            real_sql.append(s).append("\n");
                        } else {
                            String[] split = s.split(";");
                            real_sql.append(split[0]);
                            sqlList.add(real_sql.toString());
                            real_sql.delete(0, sql.length());
                            if (split.length > 1) {
                                real_sql.append(split[1]).append("\n");
                            }
                        }
                    }
                }
                if (real_sql.length() > 0 && (sqlList.isEmpty() || !real_sql.toString().equals(sqlList.get(sqlList.size() - 1)))) {
                    sqlList.add(real_sql.toString());
                }
            } else {
                sql = sql.replace(";", "");
            }

            if (hive_pro.length() > 0) {
                hive_pro.deleteCharAt(hive_pro.length() - 1); // delete latest ","
                hive_propertys = hive_pro.toString();
            }
        }

        public String getUrlWithParams() {
            StringBuilder params = new StringBuilder();
            if (null != database && !database.isEmpty()) {
                params.append(database);
            }
            params.append(";auth=noSasl;user=").append(currentUser.getUserName());
            params.append(";tenant=").append(currentUser.getTenantName()).append("?");
            for (Map.Entry<String, Object> entry : propertys.entrySet()) {
                String value = URLEncoder.encode(entry.getValue().toString());
                params.append(entry.getKey()).append("=").append(value).append(";");
            }
            params.append("kyuubi.session.group=").append(currentUser.getGroupName()).append(";");
            params.append("kyuubi.session.groupId=").append(currentUser.getGroupUuid()).append(";");
            params.append("kyuubi.session.tenant=").append(currentUser.getTenantName()).append(";");
            params.deleteCharAt(params.length() - 1); // delete latest ";"
            return gw_url + params.toString();
        }

        public List<String> getSqlWithoutConf() {
            return sqlList;
        }
    }

    public static boolean cancelQuery(String url, String user) throws IOException {
        String resInfo = gwRest(url, "DELETE", null, genHeads(user));
        Map<String, Object> results = JSON.parseObject(resInfo, Map.class);
        return (Boolean) results.get("success");
    }

    public static Map<String, Object> queryLog(String url, String user, int from, int size) throws IOException {
        String param = String.format("?from=%d&size=%d", from, size);
        String resInfo = gwRest(url + param, "GET", null, genHeads(user));
        Map<String, Object> results = JSON.parseObject(resInfo, Map.class);
        return results;
    }

    public static ArrayList<String> downloadLink(String url, String user, String output, String region, String operateId) throws IOException {
        String param = String.format("?output=%s&region=%s",
                URLEncoder.encode(output), URLEncoder.encode(tranOutputRegion(region)));
        String resInfo = gwRest(url + param, "GET", null, genHeads(user));
        JSONObject result = JSON.parseObject(resInfo).getJSONObject("urls");
        JSONArray jsonArray = result.getJSONArray(operateId);
        ArrayList<String> urls = new ArrayList<String>();
        for (Object o : jsonArray) {
            urls.add((String) o);
        }
        return urls;

    }

    public static String fileSize(String url, String user, String output, String region) throws IOException {
        String param = String.format("?output=%s&region=%s",
                URLEncoder.encode(output), URLEncoder.encode(tranOutputRegion(region)));
        String resInfo = gwRest(url + param, "GET", null, genHeads(user));
        Map<String, Object> results = JSON.parseObject(resInfo, Map.class);
        return String.valueOf(results.get("size"));
    }

    private static String gwRest(String url, String method, String postData, Map<String, String> heads) throws IOException {
        BufferedReader in = null;
        BufferedWriter writer = null;
        log.error("gwRest url: {}", url);
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("method", method);
            if (!method.toUpperCase().equals("GET")) {
                connection.setRequestProperty("method", "POST");
                connection.setDoOutput(true);
            }
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Basic Y3Jhenk6");
            connection.setConnectTimeout(900000);
            connection.setReadTimeout(900000);
            if (null != heads) {
                for (Map.Entry<String, String> entry : heads.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (null != postData && !postData.isEmpty()) {
                OutputStream outputStream = connection.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(postData);
                writer.close();
                writer = null;
            }
            connection.connect();

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line = "";
            String response = "";
            while ((line = in.readLine()) != null) {
                response += line;
            }
            log.info("response:" + response);
            return response;
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while [%s] for gateway request: %s",
                    url, CommonUtil.printStackTraceToString(e)));
            throw e;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != writer) {
                    writer.close();
                }
            } catch (Exception e2) {
                log.error(String.format("There is an exception occurred while [%s] for closing gateway: %s",
                        url, CommonUtil.printStackTraceToString(e2)));
            }
        }
    }

    private static Map<String, String> genHeads(String user) {
        Map<String, String> heads = new HashMap<>();
        String token = new String(Base64.encodeBase64(user.getBytes()));
        heads.put("Authorization", "Basic " + token);
        return heads;
    }

    public static Meta getDataSourceInfo(String deUrl, String dsName, String token,
                                         String database, GWProperties properties) throws ParseException {
        Meta meta = new Meta();
        String url = deUrl + "/actor/sources/all?name=" + URLEncoder.encode(dsName);
        Map<String, String> heads = new HashMap<>();
        heads.put("Authentication", token);
        heads.put("Cache-Control", "no-cache");
        heads.put("accept", "application/json;charset=UTF-8");
        log.info("定时查询任务来到了这里：获取数据源信息");
        if("".equals(token)){
            CurrentUser curUser = properties.getCurrentUser();
            heads.put("current_login_user", JSON.toJSONString(curUser));
            // sg1
            url = "http://data-development:80/actor/sources/all?name=" + URLEncoder.encode(dsName);
        }

        String resInfo = CommonUtil.httpResult(url, true, null, heads, properties.getCurrentUser().getUserName());
        Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
        List listObjects = JSON.parseObject(content.get("data").toString(), List.class);
        if (1 != listObjects.size()) {
            throw new ParseException("因数据源相关信息变动，请点击元数据预览右侧的刷新按钮重新获取。", 0);
        }
        log.info("定时查询任务来到了这里：成功获取数据源信息");
        Map dbObject = JSON.parseObject(listObjects.get(0).toString(), Map.class);
        String dsType = dbObject.get("sourceName").toString();
        if (dsType.equalsIgnoreCase("tidb")) {
            dsType = "mysql";
        }
        meta.setEngineType(dsType.toLowerCase());
        Map configObject = JSON.parseObject(dbObject.get("connectionConfiguration").toString(), Map.class);
        if ((null == database || database.isEmpty()) && null != configObject.get("database")) {
            database = configObject.get("database").toString();
        }
        meta.setEngineDatabase(database);
        String prop = "useSSL=false";
        if (null != configObject.get("jdbc_url_params")) {
            prop += "&" + configObject.get("jdbc_url_params").toString();
        }

        properties.setProperty("kyuubi.engine.jdbc.connection.properties",
                prop.replace("&", ","));
        String host = configObject.get("host").toString();
        String port = configObject.get("port").toString();
        String engineUrl = String.format("jdbc:%s://%s:%s/%s",
                dsType.toLowerCase(), host, port, database);
        meta.setEngineUrl(engineUrl);
        meta.setUsername(configObject.get("username").toString());
        meta.setPassword(configObject.get("password").toString());
        return meta;
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

    public static void setProperties(GWProperties gwProperties, Properties properties,
                                     GatewayConfig gatewayConfig, String engine,
                                     String region, String catalog, String database,
                                     String deUrl, String token, String output,
                                     String fileName, AccountMapper accountMapper) throws ParseException {
        String username = "";
        String password = "";
        if (engine.startsWith("spark")) {
            gwProperties.setBatchType("spark");
            gwProperties.setDatabase(database);
            gwProperties.setProperty("kyuubi.engine.type", "SPARK_SQL");
            gwProperties.setProperty("kyuubi.session.commands.tags", "type:spark-submit-sql-ds");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getSpark_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
        } else if (engine.startsWith("presto")) {
            gwProperties.setBatchType("presto");
            gwProperties.setDatabase(database);
            gwProperties.setProperty("kyuubi.engine.type", "TRINO");
            gwProperties.setProperty("kyuubi.session.engine.trino.connection.catalog", catalog);
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getTrino_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
            if (null != gwProperties.hive_propertys && !gwProperties.hive_propertys.isEmpty()) {
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.properties", gwProperties.hive_propertys);
            }
        } else if (engine.startsWith("hive")) {
            gwProperties.setBatchType("HIVE");
            gwProperties.setDatabase(database);
            gwProperties.setProperty("kyuubi.engine.type", "JDBC");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getHive_cluster_tags().get(region));
            if (null != gwProperties.hive_propertys && !gwProperties.hive_propertys.isEmpty()) {
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.properties", gwProperties.hive_propertys);
            }
            gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "HiveConnectionProvider");
        } else {
            Meta metaInfo = getDataSourceInfo(deUrl, engine, token, database, gwProperties);
            if (metaInfo.getEngineType().equals("clickhouse")) {
                if (null != database && !database.isEmpty()) {
                    gwProperties.setDatabase(database);
                }
                gwProperties.setBatchType("clickhouse");
                gwProperties.setProperty("kyuubi.engine.type", "JDBC");
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.url", metaInfo.getEngineUrl());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.user", metaInfo.getUsername());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.password", metaInfo.getPassword());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "ClickHouse2ConnectionProvider");
            } else {
                if (null != database && !database.isEmpty()) {
                    gwProperties.setDatabase(database);
                }
                gwProperties.setBatchType("mysql");
                gwProperties.setProperty("kyuubi.engine.type", "JDBC");
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.url", metaInfo.getEngineUrl());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.user", metaInfo.getUsername());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.password", metaInfo.getPassword());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "Mysql8ConnectionProvider");
            }
        }
        if (null != output && !output.isEmpty()) {
            gwProperties.setProperty("kyuubi.session.output", output);
            gwProperties.setProperty("kyuubi.session.output.region", tranOutputRegion(region));
            gwProperties.setProperty("kyuubi.session.output.filename", fileName);
        }
        gwProperties.setProperty("kyuubi.session.region", tranOutputRegion(region));
        gwProperties.setProperty("kyuubi.session.lakecat.catalog", tansLcRegion(region, engine));
        gwProperties.setProperty("kyuubi.engine.jdbc.connection.database", database);
    }

    private static String tranOutputRegion(String region) {
        if (region.equals("aws_ue1")) {
            return "us-east-1";
        } else if (region.equals("aws_sg")) {
            return "ap-southeast-1";
        } else if (region.equals("huawei_sg")) {
            return "https://obs.ap-southeast-3.myhuaweicloud.com";
        } else {
            return region.split("_")[1];
        }
    }

    private static String tansLcRegion(String region, String engine) {
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
}
