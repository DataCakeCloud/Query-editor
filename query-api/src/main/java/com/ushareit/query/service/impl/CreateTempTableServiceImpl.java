package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.CreateTempTable;
import com.ushareit.query.configuration.OlapConfig;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.mapper.CreateTempTableMapper;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.CreateTempTableService;
import com.ushareit.query.web.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * @author: huyx
 * @create: 2022-05-16 15:24
 */
@Slf4j
@Service

public class CreateTempTableServiceImpl extends AbstractBaseServiceImpl<CreateTempTable> implements CreateTempTableService{

    @Value("${olap.url.aws_ue1}")
    private String awsUrl;

    @Value("${olap.url.huawei_sg}")
    private String huaweiUrl;

    @Value("${olap.url.aws_sg}")
    private String awsSGUrl;

    @Autowired
    private OlapConfig olapUrl;

    @Value("${cloud.aws.us-east-1.ue1_aws_access_keys_id}")
    private String ue1_aws_access_keys_id;

    @Value("${cloud.aws.us-east-1.ue1_aws_secret_access_key}")
    private String ue1_aws_secret_access_key;

    //@Value("${cloud.aws.us-east-1.ue1_aws_bucket}")
    //private String ue1_aws_bucket;

    //@Value("${cloud.aws.us-east-1.ue1_aws_region}")
    //private String ue1_aws_region;

    @Value("${cloud.huawei.obs_access_key_id}")
    private String obs_access_key_id;

    @Value("${cloud.huawei.obs_secret_access_key}")
    private String obs_secret_access_key;

    @Value("${cloud.huawei.obs_bucket}")
    private String obs_bucket;

    @Value("${cloud.ks.ks_access_key_id}")
    private String ks_access_key_id;

    @Value("${cloud.ks.ks_secret_access_key}")
    private String ks_secret_access_key;

    @Value("${cloud.ks.ks_endpoint}")
    private String ks_endpoint;

    @Value("${cloud.huawei.obs_endPoint}")
    private String obs_endPoint;

    @Value("${upload_local_tmp}")
    private String upload_local_tmp;

    @Value("${cluster-manager.url}")
    private String urlClusterManager;

    @Value("${cluster-manager.s3_role_url}")
    private String urlS3RoleUrl;

    @Resource
    private CreateTempTableMapper createTempTableMapper;

    @Resource
    private AccountMapper accountMapper;

    @Override
    public CrudMapper<CreateTempTable> getBaseMapper() { return createTempTableMapper; }

    public void handleParams(Map<String, Object> params, String filename,
    		String userInfo, int tenantId) {
        params.put("filename", filename);
        params.put("upload_local_tmp", upload_local_tmp);
        String userRegion = (String)params.get("region");
        if (!params.get("engine_key").equals("presto_huawei") && !params.get("engine_key").equals("spark-submit-sql-3_huawei_ap-southeast-3")) {
            params.put("ue1_aws_access_keys_id", ue1_aws_access_keys_id);
            params.put("ue1_aws_secret_access_key", ue1_aws_secret_access_key);
            //params.put("ue1_aws_bucket", ue1_aws_bucket);
            //params.put("ue1_aws_region", ue1_aws_region);
            String provider = "";
            String region = "";
            try {
                String url = urlClusterManager + "/cluster-service/cloud/resource/search?&pageNum=1&pageSize=100";
                String resInfo = ClusterManagerUtil.getClusterManagerInfo(url, userInfo);
                Map content = JSON.parseObject(resInfo, Map.class);
                Map data = JSON.parseObject(content.get("data").toString(), Map.class);
                List listTenant = JSON.parseObject(data.get("list").toString(), List.class);
                for (int i = 0; i < listTenant.size(); ++i) {
                    Map tenant = JSON.parseObject(listTenant.get(i).toString(), Map.class);
                    //if (Integer.valueOf((String)tenant.get("tenantId")) == tenantId) {
                        /*String name = (String)tenant.get("name");
                        if (!name.equalsIgnoreCase(userRegion)) {
                        	continue;
                        }*/
                        String cur_region = "";
                        provider = (String)tenant.get("provider");
                        region = (String)tenant.get("region");
                        cur_region = provider + "_" + region;
                        if (!cur_region.equalsIgnoreCase(userRegion)) {
                        	continue;
                        }
                        String s3Path = (String)tenant.get("storage");
                        if (!s3Path.substring(s3Path.length() - 1).equals("/")) {
                        	s3Path += "/";
                        }
                        int bucketStart = s3Path.indexOf("//") + 2;
                        int bucketEnd = s3Path.indexOf("/", bucketStart);
                        String bucket = s3Path.substring(bucketStart, bucketEnd);
                        String prefix = "";
                        if (s3Path.length() > bucketEnd + 1) {
                        	prefix = s3Path.substring(bucketEnd + 1);
                        }
                        if (s3Path.startsWith("ks")) {
                            provider = "ksyun";
                        }
                        params.put("ue1_aws_bucket", bucket);
                        params.put("ue1_aws_region", region);
                        params.put("ue1_aws_prefix", prefix);
                        params.put("cloud_provider", provider);
                        log.info(String.format("create temp table parse s3 bucket: %s, region: %s, prefix: %s",
                        		bucket, region, prefix));
                        if (provider.equals("googlecloud")) {
                            return;
                        } else if (provider.equals("ksyun")) {
                            params.put("ks_access_key_id", ks_access_key_id);
                            params.put("ks_secret_access_key", ks_secret_access_key);
                            params.put("ks_endpoint", ks_endpoint);
                            return;
                        } else {
                            break;
                        }
                    //}
                }

                String resRoleInfo = ClusterManagerUtil.getClusterManagerInfo(
                        String.format(urlS3RoleUrl, provider, region), userInfo);
                Map roleContent = JSON.parseObject(resRoleInfo, Map.class);
                String roleData = roleContent.get("data").toString();
                params.put("s3_role_data", roleData);
                log.info(String.format("create temp table parse s3 role: %s", roleData));
        	} catch (Exception e) {
                log.error(String.format("There is an exception occurred while parse cluster info: %s",
                		CommonUtil.printStackTraceToString(e)));
                throw e;
            }
        } else {
            params.put("obs_access_key_id", obs_access_key_id);
            params.put("obs_secret_access_key", obs_secret_access_key);
            params.put("obs_bucket", obs_bucket);
            params.put("obs_endPoint", obs_endPoint);
            params.put("cloud_provider", "huawei");
        }

//        params.putIfAbsent("lifeCycle", 1);
    }

    @Override
    public HashMap<String, Object> uploadNew(Map<String, Object> params, String user,
    		MultipartFile multipartFile, String userInfo, int tenantId) {
        String filename = multipartFile.getOriginalFilename();
        log.info(String.format("%s start to upload file[engine_key=%s, filename=%s, lifeCycle=%s, fileSize=%s]", user, params.get("engine_key"), filename, params.get("lifeCycle"), FileUtil.getStringSize(multipartFile.getSize())));
        HashMap<String, Object> response = new HashMap<>();

        String name = filename.substring(0, filename.lastIndexOf("."));
        String suffix = filename.substring(filename.lastIndexOf(".") + 1);
        if (suffix.equals("csv") || suffix.equals("xls") || suffix.equals("xlsx")) {
            if (suffix.equals("xls") || suffix.equals("xlsx")) {
                filename = name + ".csv";
            }
            handleParams(params, filename, userInfo, tenantId);

            try {
                ArrayList<Map<String, Object>> meta = ParseFileHeaderUtil.readFileHeader(multipartFile);
                log.info(String.format("The file header of %s is parsed successfully by %s in %s", params.get("filename"), user, params.get("engine_key")));

                String start = TimeUtil.getNow();
                String location = CloudUtil.upload(params, user, multipartFile);
                String end = TimeUtil.getNow();
                float updateTime = TimeUtil.getTimeDiff(start, end);
                log.info(String.format("The file of %s is uploaded to %s successfully by %s, it takes %sS", filename, params.get("engine_key"), user, updateTime));

                HashMap<String, Object> data = new HashMap<>();
                data.put("status", 0);
                data.put("message", "上传成功");
                data.put("location", location);
                data.put("meta", meta);
                response.put("code", 0);
                response.put("data", data);
            } catch (Exception e) {
                log.error(String.format("%s failed to upload file to cloud[engine=%s, file=%s]: %s", user, params.get("engine_key"), filename, CommonUtil.printStackTraceToString(e)));
                try {
                    throw e;
                } catch (Exception ex) {
                    String message = ex.getMessage().trim();
                    log.error(String.format("There is an Exception when %s upload file[engine=%s, %s] to cloud: %s", user, params.get("engine_key"), filename, message));
                    response.put("code", "72");
                    response.put("message", message);
                }
            }
        } else {
            String message = String.format("The format of uploading file[engine=%s, file=%s] is incorrect: %s", params.get("engine_key"), filename, suffix);
            log.error(String.format("[%s]: %s", user, message));
            response.put("code", "71");
            response.put("message", message);
        }
        return response;
    }

    @Override
    public HashMap<String, Object> uploadRepeat(Map<String, Object> params, String user,
    		MultipartFile multipartFile, String userInfo, int tenantId, String tenantName) {
        HashMap<String, Object> response = new HashMap<>();
        String filename = multipartFile.getOriginalFilename();
        log.info(String.format("%s start to upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s, fileSize=%s]", user, params.get("engine_key"), params.get("database"), params.get("tables"), filename, FileUtil.getStringSize(multipartFile.getSize())));

        List<Account> account = accountMapper.listAll();
//        JSONObject groupAccount = CommonUtil.getUserGroup(user, account, adminUsername, adminPassword);
//        String group = groupAccount.getString("group");
//        String username = groupAccount.getString("username");
//        String password = groupAccount.getString("password");
        String username = "";
        String password = "";
        for (int i = 0; i < account.size(); i++) {
            if (account.get(i).getUserGroup().equals("BDP")) {
                username = account.get(i).getUsername();
                password = account.get(i).getPassword();
                break;
            }
        }
//        if (group.equals("")) {
//            String message = String.format("%s is not assigned a group[%s]", user, group);
//            response.put("code", 80);
//            response.put("message", String.format("%s, %s", message, "please contact the administrator!"));
//            log.error(String.format("%s when upload file repeatedly[engine=%s, table=%s， file=%s]", message, params.get("database"), params.get("table"), filename));
//            return response;
//        }

        Statement statement = null;
        Connection connection = null;

        handleParams(params, filename, userInfo, tenantId);
        try {
            String url = "";
            String region = params.get("region").toString();
            boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
            Properties properties = new Properties();
            properties.setProperty("user", username);
            if (params.get("engine_key").equals("presto_aws") || params.get("engine_key").equals("spark-submit-sql-3_aws_us-east-1") || params.get("engine_key").equals("ares_ue1")) {
                url = awsUrl;
                if (use_ssl) {
                    properties.setProperty("password", password);
                }
                properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
            } else if (params.get("engine_key").equals("presto_huawei") || params.get("engine_key").equals("spark-submit-sql-3_huawei_ap-southeast-3")) {
                url = huaweiUrl;
            } else if (params.get("engine_key").toString().startsWith("presto")
                    && !params.get("engine_key").equals("presto_aws")) {
                url = olapUrl.getUrl().get(region);
                if (use_ssl) {
                    properties.setProperty("password", password);
                }
                properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
            } else {
                url = awsSGUrl;
                if (use_ssl) {
                    properties.setProperty("password", password);
                }
                properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
            }

            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            ResultSet rs = null;

            log.info(String.format("%s start to query file lifeCycle when upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s]", user, params.get("engine_key"), params.get("database"), params.get("tables"), filename));
            String lifeCycleSql = String.format("--conf bdp-query-tenancy=%s\nshow create table %s.%s",
                    tenantName, params.get("database"), params.get("table"));
            rs = statement.executeQuery(lifeCycleSql);
            String lifeCycle = "1";
            while(rs.next()) {
                for(int i=0; i<rs.getMetaData().getColumnCount(); i++){
                    String info = rs.getString(i+1);
                    lifeCycle = info.split("LOCATION")[1].trim().split("/months_")[1].split("/")[0];
                }
            }
            log.info(String.format("%s successfully get the lifeCycle when upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s, lifeCycle=%s]", user, params.get("engine_key"), params.get("database"), params.get("tables"), filename, lifeCycle));
            params.put("lifeCycle", lifeCycle);

            String start = TimeUtil.getNow();
            String location = CloudUtil.upload(params, user, multipartFile);
            String end = TimeUtil.getNow();
            float updateTime = TimeUtil.getTimeDiff(start, end);
            log.info(String.format("%s successfully uploaded file repeatedly[engine=%s, filename=%s], it takes %sS", user, params.get("engine_key"), filename, updateTime));

            String sql = String.format("--conf bdp-query-tenancy=%s\nalter table %s.%s set location '%s'",
                    tenantName, params.get("database"), params.get("table"), location);
            if (params.get("engine_key").equals("presto_aws_sg") || params.get("engine_key").equals("spark-submit-sql-3_aws_ap-southeast-1") || params.get("engine_key").equals("ares_ap1")) {
                sql = "--conf bdp-query-engine=ares_ap1\n" + sql;
            }
            rs = statement.executeQuery(sql);

            rs.close();
            statement.close();
            connection.close();
            log.info(String.format("%s successfully modified the file location of the table when upload file repeatedly[engine=%s, table=%s, location=%s]", user, params.get("engine_key"), params.get("table"), location));

            HashMap<String, Object> data = new HashMap<>();
            data.put("status", 0);
            data.put("message", "重复上传成功");
            response.put("code", 0);
            response.put("data", data);
        } catch (Exception e) {
            log.error(String.format("%s failed to upload file repeatedly to cloud[engine=%s, file=%s]: %s", user, params.get("engine_key"), filename, CommonUtil.printStackTraceToString(e)));
            try {
                throw e;
            } catch (Exception ex) {
                String message = ex.getMessage().trim();
                log.error(String.format("There is an Exception when %s upload file repeatedly[engine=%s, file=%s] to cloud: %s", user, params.get("engine_key"), filename, message));
                response.put("code", 82);
                response.put("message", message);
            }
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                code = code>0 ? code: 81;
                String message = ex.getMessage().trim();
                log.error(String.format("%s failed to close[engine=%s, file=%s, code=%d] when upload file repeatedly: %s", user, params.get("engine_key"), filename, code, message));
                response.put("code", code);
                response.put("message", message);
            }
        }


        return response;
    }

    @Override
    public String nameCheck(List<String> tableList, String tableName) {
        String isExisted = "0";

        if (tableList.contains(tableName)) {
            isExisted = "1";
        }

        return isExisted;
    }

    @Override
    public HashMap<String, Object> execute(String user, String engine, String database, String table, String comment, String location, List<Object> field, String tenantName, String region) {
        log.info(String.format("%s starts to create temp table %s.%s in %s", user, database, table, engine));
        HashMap<String, Object> response = new HashMap<>();

        String sqlTemp = "--conf bdp-query-user=%s\n--conf bdp-query-tenancy=%s\n" +
                "CREATE TABLE IF NOT EXISTS %s.%s\n" +
                "(\n" +
                "%s \n" +
                ")\n" +
                "COMMENT '%s' \n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "tblproperties('skip.header.line.count'='1') \n" +
                "LOCATION '%s' ";

        if (engine.equals("presto_aws_sg") || engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1")) {
            String createSql = "--conf bdp-query-user=%s\n--conf bdp-query-tenancy=%s\n" +
                    "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s\n" +
                    "(\n" +
                    "%s \n" +
                    ")\n" +
                    "COMMENT '%s' \n" +
                    "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                    "LOCATION '%s' \n" +
                    "tblproperties('skip.header.line.count'='1')";
            // sqlTemp = "--conf bdp-query-engine=ares_ap1\n" + createSql;
            sqlTemp = createSql;
        }

        ArrayList fieldList = new ArrayList<>();
        for (int i=0; i < field.size(); i++) {
            JSONObject fieldJson = JSONObject.parseObject(field.get(i).toString());
            String fieldName = fieldJson.getString("name");
            String fieldType = fieldJson.getString("type");
            fieldList.add(String.format("%s %s", fieldName, fieldType));
        }

        String fieldString = String.join(",\n", fieldList);

        String sql = String.format(sqlTemp, user, tenantName, database, table, fieldString, comment, location);
        log.info(String.format("%s create temp table as \n %s", user, sql));

        Statement statement = null;
        Connection connection = null;
        Properties properties = new Properties();
        String url = "";

        List<Account> account = accountMapper.listAll();

//        JSONObject groupAccount = CommonUtil.getUserGroup(user, account, adminUsername, adminPassword);
//        String group = groupAccount.getString("group");
//        String username = groupAccount.getString("username");
//        String password = groupAccount.getString("password");
        String username = "";
        String password = "";
        for (int i = 0; i < account.size(); i++) {
            if (account.get(i).getUserGroup().equals("BDP")) {
                username = account.get(i).getUsername();
                password = account.get(i).getPassword();
                break;
            }
        }

//        if (group.equals("")) {
//            String message = String.format("%s is not assigned a group[%s]", user, group);
//            response.put("code", 12);
//            response.put("message", String.format("%s, %s", message, "please contact the administrator!"));
//            log.error(String.format("%s when creating temp table %s.%s", message, database, table));
//            return response;
//        }

        boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
        properties.setProperty("user", username);
        if (engine.equals("presto_aws") || engine.equals("spark-submit-sql-3_aws_us-east-1") || engine.equals("ares_ue1")) {
            url = awsUrl;
            if (use_ssl) {
                properties.setProperty("password", password);
            }
            properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
        } else if (engine.equals("presto_huawei") || engine.equals("spark-submit-sql-3_huawei_ap-southeast-3")) {
            url = huaweiUrl;
        } else if (engine.startsWith("presto")
                && !engine.equals("presto_aws")) {
            url = olapUrl.getUrl().get(region);
            if (use_ssl) {
                properties.setProperty("password", password);
            }
            properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
        } else {
            url = awsSGUrl;
            if (use_ssl) {
                properties.setProperty("password", password);
            }
            properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
        }

        try {
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);

            rs.close();
            statement.close();
            connection.close();

            response.put("code", 0);
            response.put("data", "SUCCESS");

            log.info(String.format("%s creates temp table %s.%s successfully", user, database, table));

        } catch (Exception e) {
            int code = 13;
            log.error(String.format("There is a stack err when %s creating temp table[database=%s, table=%s, errCode=%d]: %s", user, database, table, code, CommonUtil.printStackTraceToString(e)));

            try {
                throw e;
            } catch (SQLException | DataIntegrityViolationException ex) {
                String message = "";
                if (ex.getClass() == SQLException.class) {
                    code = ((SQLException) ex).getErrorCode();
                    message = ex.getMessage().trim();
                    log.error(String.format("There is a SQLException when %s creating temp table[database=%s, table=%s, errCode=%d]: %s", user, database, table, code, message));
                } else {
                    message = ex.getMessage().trim();
                }
                log.error(String.format("%s failed to create temp table[database=%s, table=%s, errCode=%d]: %s", user, database, table, code, message));
                if (code == 2) {
                    HashMap<String, Object> data = new HashMap<>();
                    data.put("status", code);
                    data.put("message", message);
                    response.put("code", code);
                    response.put("data", data);
                } else {
                    response.put("code", code);
                    response.put("message", message);
                }

                return response;
            }
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                code = code>0 ? code: 14;
                String message = ex.getMessage().trim();
                response.put("code", code);
                response.put("message", message);
            }
        }
        return response;
    }
}
