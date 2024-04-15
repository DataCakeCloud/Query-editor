package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.CreateTempTable;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.configuration.GatewayConfig;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.mapper.CreateTempTableMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.CreateTempTableService;
import com.ushareit.query.service.TempTableGWService;
import com.ushareit.query.web.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.sql.*;
import java.text.ParseException;
import java.util.*;

@Slf4j
@Service
public class TempTableGWServiceImpl extends AbstractBaseServiceImpl<CreateTempTable> implements TempTableGWService {
    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${de.gateway}")
    private String deUrl;

    @Autowired
    private GatewayConfig gatewayConfig;

    @Resource
    private CreateTempTableMapper createTempTableMapper;

    @Resource
    private CreateTempTableServiceImpl createTempTableService;

    @Resource
    private AccountMapper accountMapper;

    @Override
    public CrudMapper<CreateTempTable> getBaseMapper() { return createTempTableMapper; }

    @Override
    public HashMap<String, Object> uploadRepeat(Map<String, Object> params, MultipartFile multipartFile,
                                                String userInfo, CurrentUser currentUser) throws ParseException {
        String user = currentUser.getUserName();
        int tenantId = currentUser.getTenantId();
        String region = (String)params.get("region");
        String engine = params.get("engine_key").toString();
        HashMap<String, Object> response = new HashMap<>();
        String filename = multipartFile.getOriginalFilename();
        log.info(String.format("%s start to upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s, fileSize=%s]",
                user, params.get("engine_key"), params.get("database"), params.get("tables"),
                filename, FileUtil.getStringSize(multipartFile.getSize())));


        Properties properties = new Properties();
        GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
        GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, params.get("catalog").toString(),
                params.get("database").toString(),
                deUrl, "", "", "", accountMapper);

        Statement statement = null;
        Connection connection = null;
        ResultSet rs = null;
        createTempTableService.handleParams(params, filename, userInfo, tenantId);
        try {
            connection = DriverManager.getConnection(gwProperties.getUrlWithParams(), properties);
            statement = connection.createStatement();

            log.info(String.format("%s start to query file lifeCycle when upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s]", user, params.get("engine_key"), params.get("database"), params.get("tables"), filename));
            String lifeCycleSql = String.format("show create table %s.%s",
                    params.get("database"), params.get("table"));
            rs = statement.executeQuery(lifeCycleSql);
            String lifeCycle = "1";
            while(rs.next()) {
                for(int i=0; i<rs.getMetaData().getColumnCount(); i++){
                    String info = rs.getString(i+1);
                    lifeCycle = info.split("LOCATION")[1].trim().split("/months_")[1].split("/")[0];
                }
            }
            log.info(String.format("%s successfully get the lifeCycle when upload file repeatedly[engine_key=%s, database=%s, table=%s, filename=%s, lifeCycle=%s]",
                    user, params.get("engine_key"), params.get("database"),
                    params.get("tables"), filename, lifeCycle));
            params.put("lifeCycle", lifeCycle);

            String start = TimeUtil.getNow();
            String location = CloudUtil.upload(params, user, multipartFile);
            String end = TimeUtil.getNow();
            float updateTime = TimeUtil.getTimeDiff(start, end);
            log.info(String.format("%s successfully uploaded file repeatedly[engine=%s, filename=%s], it takes %sS", user, params.get("engine_key"), filename, updateTime));

            String sql = String.format("alter table %s.%s set location '%s'",
                    params.get("database"), params.get("table"), location);
            rs = statement.executeQuery(sql);

            log.info(String.format("%s successfully modified the file location of the table when upload file repeatedly[engine=%s, table=%s, location=%s]", user, params.get("engine_key"), params.get("table"), location));

            HashMap<String, Object> data = new HashMap<>();
            data.put("status", 0);
            data.put("message", "重复上传成功");
            response.put("code", 0);
            response.put("data", data);
        } catch (Exception e) {
            log.error(String.format("%s failed to upload file repeatedly to cloud[engine=%s, file=%s]: %s",
                    user, params.get("engine_key"), filename, CommonUtil.printStackTraceToString(e)));
            try {
                throw e;
            } catch (Exception ex) {
                String message = ex.getMessage().trim();
                log.error(String.format("There is an Exception when %s upload file repeatedly[engine=%s, file=%s] to cloud: %s", user, params.get("engine_key"), filename, message));
                response.put("code", 82);
                response.put("message", message);
            }
        }
        return response;
    }

    @Override
    public HashMap<String, Object> execute(String database, String table, String comment,
                                           String location, List<Object> field, String engine,
                                           String region, CurrentUser currentUser) throws ParseException {
        String user = currentUser.getUserName();
        log.info(String.format("%s starts to create temp table %s.%s in %s",
                user, database, table, region));
        HashMap<String, Object> response = new HashMap<>();

        String sqlTemp = "CREATE TABLE IF NOT EXISTS %s.%s\n" +
                "(\n" +
                "%s \n" +
                ")\n" +
                "COMMENT '%s' \n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "LOCATION '%s' \n" +
                "tblproperties('skip.header.line.count'='1')";

        if (region.equals("aws_sg")) {
            sqlTemp = "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s\n" +
                    "(\n" +
                    "%s \n" +
                    ")\n" +
                    "COMMENT '%s' \n" +
                    "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                    "LOCATION '%s' \n" +
                    "tblproperties('skip.header.line.count'='1')";
        }

        ArrayList fieldList = new ArrayList<>();
        for (int i=0; i < field.size(); i++) {
            JSONObject fieldJson = JSONObject.parseObject(field.get(i).toString());
            String fieldName = fieldJson.getString("name");
            String fieldType = fieldJson.getString("type");
            fieldList.add(String.format("%s %s", fieldName, fieldType));
        }

        String fieldString = String.join(",\n", fieldList);

        String sql = String.format(sqlTemp, database, table, fieldString, comment, location);
        log.info(String.format("%s create temp table as \n %s", user, sql));

        Properties properties = new Properties();
        GatewayUtil.GWProperties gwProperties = new GatewayUtil.GWProperties(currentUser, gatewayUrl, "");
        GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, "iceberg", database,
                deUrl, "", "", "", accountMapper);

        Statement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = DriverManager.getConnection( gwProperties.getUrlWithParams(), properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);

            response.put("code", 0);
            response.put("data", "SUCCESS");

            log.info(String.format("%s creates temp table %s.%s successfully", user, database, table));
        } catch (Exception e) {
            int code = 13;
            log.error(String.format("There is a stack err when %s creating temp table[database=%s, table=%s, errCode=%d]: %s", user, database, table, code, CommonUtil.printStackTraceToString(e)));
            String message = e.getMessage().trim();
            log.error(String.format("%s failed to create temp table[database=%s, table=%s, errCode=%d]: %s",
                    user, database, table, code, message));
            response.put("code", code);
            response.put("message", message);
        }
        return response;
    }
}
