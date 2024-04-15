package com.ushareit.query.service.impl;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.net.URLEncoder;

import com.ushareit.query.bean.*;
import com.ushareit.query.configuration.OlapConfig;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.ProbeConstant;
import com.ushareit.query.mapper.*;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.TaskService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.web.utils.*;
import com.shareit.sharesql.core.SqlTransformer;
import com.shareit.sharesql.core.SqlResult;
import io.prestosql.jdbc.PrestoResultSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.ushareit.query.exception.ServiceException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URLDecoder;

/**
 * @author: tianxu
 * @create: 2022-02-10
 */

@Slf4j
@Service
//@ConfigurationProperties(prefix = "engine")
//@Setter
public class TaskServiceImpl extends AbstractBaseServiceImpl<QueryHistory> implements TaskService {
    @Value("${olap.url.aws_ue1}")
    private String awsUrl;

    @Value("${olap.url.huawei_sg}")
    private String huaweiUrl;

    @Value("${olap.url.aws_sg}")
    private String awsSGUrl;

    @Autowired
    private OlapConfig olapUrl;

    @Value("${count.display}")
    private int displayCount;

    @Value("${count.download}")
    private int downloadCount;

    @Value("${cancel.sleep}")
    private int cancelSleep;

    @Value("${cancel.count}")
    private int cancelCount;

    @Value("${spark.sleep}")
    private int sparkSleep;

    @Value("${spark.count}")
    private int sparkCount;

    @Value("${spark.cancelCount}")
    private int sparkCancelCount;

    @Value("${olapDB.url}")
    private String olapDbUrl;

    @Value("${olapDB.username}")
    private String olapDbUser;

    @Value("${olapDB.password}")
    private String olapDbPwd;

    @Value("${databend.engineKey}")
    private String databendEngineKey;

    @Value("${aiservice.smart_engine}")
    private String smartEngineUrl;

    @Value("${aiservice.timeest}")
    private String timeestUrl;
    @Value("${genie.client.url}")
    private String genieUrl;

    @Value("${gateway.logDir}")
    private String gatewayLogDir;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Resource
    private SavedQueryMapper savedQueryMapper;

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private AccountMapper accountMapper;

    @Override
    public CrudMapper<QueryHistory> getBaseMapper() {
        return queryHistoryMapper;
    }

    @Resource
    private ShareGradeMapper sgMapper;

    @Resource
    private ErrorInfoMapper erMapper;

    @Resource
    private ExperimentPilotMapper pilotMapper;

    @Resource
    private QueryResultMapper queryResultMapper;

    @Resource
    private TransSqlMapper tsMapper;

    private List<ErrorInfo> listEr;
    private List<Pattern> listRegex;

    private List<ExperimentPilot> listPilot;
    private long lastUpdatePilot;

//    List<Map<String, String>> basic;
//    Map<String, ArrayList<Map<String, String>>> obj;
//    Map<String, Map<String, String>> account;

    @PostConstruct
    public void initRegex() {
        listEr = erMapper.selectAll();
        listRegex = new ArrayList<Pattern>();
        for (int i = 0; i < listEr.size(); i++) {
            log.info(String.format("add regex %s", listEr.get(i).getErorRegex()));
            Pattern r = Pattern.compile(listEr.get(i).getErorRegex());
            listRegex.add(r);
        }

        listPilot = pilotMapper.selectAll();
        lastUpdatePilot = System.currentTimeMillis();
    }

    @Override
    public Object save(QueryHistory queryHistory) {
        // 保存部分查询信息
        super.save(queryHistory);

        return queryHistory;
    }

    @Override
    public void update(QueryHistory queryHistory) {
        // 更新部分查询信息
        super.update(queryHistory);
    }

    @Override
    public boolean isInExpBlacklist(int exp_id, String name) {
        long curTime = System.currentTimeMillis();
        if (curTime - lastUpdatePilot > 30000) {
            listPilot = pilotMapper.selectAll();
            lastUpdatePilot = curTime;
        }
        for (int i = 0; i < listPilot.size(); i++) {
            ExperimentPilot ep = listPilot.get(i);
            if (ep.getExp_id() == exp_id && name.equals(ep.getUser_name())) {
                return ep.getAction() == 0;
            }
        }
        return false;
    }

    @Override
    public boolean isInExpWhitelist(int exp_id, String name) {
        long curTime = System.currentTimeMillis();
        if (curTime - lastUpdatePilot > 30000) {
            listPilot = pilotMapper.selectAll();
            lastUpdatePilot = curTime;
        }
        for (int i = 0; i < listPilot.size(); i++) {
            ExperimentPilot ep = listPilot.get(i);
            if (ep.getExp_id() == exp_id && name.equals(ep.getUser_name())) {
                return ep.getAction() == 1;
            }
        }
        return false;
    }

    private int checkUuid(QueryHistory queryHistory) {
        // 校验任务标识uuid是否唯一
        try {
            super.checkOnUpdate(super.getByUuid(queryHistory.getUuid()), queryHistory);
        } catch (Exception e) {
            log.error(String.format("[%s] uuid[%s] is not unique: %s", queryHistory.getCreateBy(), queryHistory.getUuid(), e.getMessage()));
            return 11;
        }
        return 0;
    }

    public String filesize(String url) {
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("method", "GET");
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line = "";
            String response = "";
            while ((line = in.readLine()) != null) {
                response += line;
            }
            log.info(response);

            Map content = JSON.parseObject(response, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            return (String) data.get("fileSize");
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while calculating[%s] file size for http request: %s", url, CommonUtil.printStackTraceToString(e)));
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                log.error(String.format("There is an exception occurred while calculating[%s] file size for closing http: %s", url, CommonUtil.printStackTraceToString(e2)));
            }
        }
        return "-1";
    }

    private String getDomain(String url, String engine) {
        String http = "";
        if (url.indexOf(":443") == -1) {
            http = url.replace("jdbc:presto", "http");
        } else {
            http = url.replace("jdbc:presto", "https");
        }
        return http.split("hive")[0];
    }

    private ArrayList<Map<String, Object>> getResult(ResultSet rs, ArrayList<String> column,
                                                     ArrayList<Map<String, String>> types, boolean history) throws SQLException {
        int count = 0;
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        while (rs.next()) {
            if (count < displayCount) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                    String valStr = rs.getString(i + 1);
                    String col_name = column.get(i).toString();
                    if (null != types && types.size() > i && null != valStr) {
                        String col_type = types.get(i).get(col_name);
                        if (null != col_type) {
                            if (col_type.equalsIgnoreCase("integer") ||
                                    col_type.equalsIgnoreCase("smallint") ||
                                    col_type.equalsIgnoreCase("tinyint")) {
                                row.put(col_name, Long.valueOf(valStr));
                                continue;
                            } else if (col_type.equalsIgnoreCase("bigint")) {
                                row.put(col_name, Long.valueOf(valStr));
                                continue;
                            } else if (col_type.equalsIgnoreCase("double") ||
                                    col_type.equalsIgnoreCase("real") ||
                                    col_type.equalsIgnoreCase("float")) {
                                row.put(col_name, Double.valueOf(valStr));
                                continue;
                            } else if (col_type.equalsIgnoreCase("boolean")) {
                                row.put(col_name, Boolean.valueOf(valStr));
                                continue;
                            }
                        }
                    }
                    row.put(col_name, valStr);
                }
                result.add(row);
                count += 1;
            } else if (count < downloadCount && !history) {
                count += 1;
            } else {
                break;
            }
        }
        return result;
    }

    private ArrayList<Map<String, Object>> getMysqlResult(ResultSet rs, ArrayList<String> column, boolean history) throws SQLException {
        int count = 0;
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        while (rs.next()) {
            if (count < downloadCount && !history) {
                count += 1;
            } else {
                break;
            }
        }
        return result;
    }

    private String retrieveSQLExceptionCause(SQLException ex) {
        String result = "";
        Throwable ex2 = ex.getCause().getCause();
        if (ex2.getCause().getCause() == null) {
            if (ex2.getCause() == null || ex2.getCause().getMessage() == null) {
                result = ex2.getMessage();
            } else {
                result = ex2.getCause().getMessage();
            }
        } else {
            result = ex2.getCause().getCause().getMessage();
            if (result == null) {
                result = ex2.getCause().getMessage();
                if (result.length() < 10 || result == null) {
                    result = ex2.getMessage();
                }
            }
        }
        return result.trim();
    }


    @Override
    public HashMap<String, Object> execute(String uuid, String engineOriginal, String querySql,
                                           Integer isDatabend, Integer confirmedSmart, String user, String region, String catalog,
                                           String database, String groupId, int tenantId, String tenantName, Integer taskId) throws ParseException, SQLException {
        String start = TimeUtil.getNow();

        String engine = engineOriginal;

        log.info(String.format("%s start to execute query[uuid=%s, engine=%s]", user, uuid, engine));
        QueryHistory queryHistory = new QueryHistory();
        HashMap<String, Object> response = new HashMap<>();

        queryHistory.setCreateBy(user);
        queryHistory.setUpdateBy(user);
        queryHistory.setEngine(engineOriginal);
        queryHistory.setQuerySql(querySql);
        queryHistory.setStartTime(Timestamp.valueOf(start));
        queryHistory.setIsDatabend(isDatabend);
        queryHistory.setRegion(region);
        queryHistory.setCatalog(catalog);
        queryHistory.setGroupId(groupId);
        queryHistory.setTenantId(tenantId);
        queryHistory.setFromOlap(true);
        queryHistory.setTaskId(taskId);

        queryHistory.setUuid(uuid);
        if (checkUuid(queryHistory) == 11) {
            response.put("code", 11);
            response.put("message", "uuid is not unique");
            return response;
        }

//        String upperSql = querySql.trim().toUpperCase();
//        if (upperSql.startsWith("CREATE ")) {
//            String message = "暂不支持创建数据库表";
//            queryHistory.setStatus(1);
//            queryHistory.setStatusZh(message);
//            save(queryHistory);
//            response.put("code", 61);
//            response.put("message", message);
//            log.error(String.format("[%s]%s when executing query task[%s]", user, message, uuid));
//            return response;
//        }

        String url = "";
        String queryId = "";
        String delimeter = "_";
        String sql = querySql;
        Statement statement = null;
        Connection connection = null;
        Properties properties = new Properties();
        String[] engineConfig = engine.split(delimeter);

//        String group = "";
        String username = "";
        String password = "";

        log.info(String.format("%s start to get config when executing query[uuid=%s]", user, uuid));
        String conf = String.format("--conf bdp-query-request-id=%s\n--conf bdp-query-user=%s\n", uuid, user);
        conf += String.format("--conf bdp-query-tenancy=%s\n--conf bdp-query-tenantid=%d\n", tenantName, tenantId);
        if (1 == confirmedSmart && engine.startsWith("spark")) {
            Map<String, String> statusObj = transSQLtoSpark(querySql, user);
            if (statusObj.get("res").equals("SUCCESS")) {
                querySql = statusObj.get("sparkSql");
            }
        }
        if (engine.startsWith("smart")) {
            sql = conf + "--conf olap-engine-mode=smart\n" + sql;
        } else if (engine.startsWith("presto") || engine.startsWith("spark")) {
            if (engine.startsWith("spark")) {
                if (!engine.equals("spark-submit-sql-3_aws_us-east-1")) {  //非spark美东的，不进行路由跳转到awsUrl
                    conf += "--conf olap-engine-routing=FALSE\n";
                }
                List<Account> account = accountMapper.listAll();
//                String sparkUsername = "";
//                String sparkPassword = "";
//                for (Account value : account) {
//                    if (value.getUserGroup().equals("spark")) {
//                        sparkUsername = value.getUsername();
//                        sparkPassword = value.getPassword();
//                        break;
//                    }
//                }
//                JSONObject groupAccount = CommonUtil.getSparkUserGroup(user, account, sparkUsername, sparkPassword);
//                group = groupAccount.getString("group");
//                username = groupAccount.getString("username");
//                password = groupAccount.getString("password");
                for (int i = 0; i < account.size(); i++) {
                    if (account.get(i).getUserGroup().equals("spark")) {
                        username = account.get(i).getUsername();
                        password = account.get(i).getPassword();
                        break;
                    }
                }

                conf += String.format(
//                        "--conf bdp-query-group=%s\n" +
                        "--conf bdp-query-engine=%s\n" +
                                "--conf bdp-query-provider=%s\n" +
                                "--conf bdp-query-region=%s\n", engineConfig[0], engineConfig[1], engineConfig[2]
                );
                sql = conf + querySql;
            } else {
                sql = conf + sql;
            }
        } else {
            sql = conf + String.format("--conf bdp-query-engine=%s\n%s", engine, querySql);
//            if (engine.startsWith("ch")) {
//                ArrayList<Map<String, String>> engineList = obj.get("ClickHouse");
//                engine_label = getCustomLabel(engineList, engine);
//            } else if (engine.startsWith("tidb")) {
//                ArrayList<Map<String, String>> engineList = obj.get("Tidb");
//                engine_label = getCustomLabel(engineList, engine);
//            } else if (engine.startsWith("mysql")) {
//                ArrayList<Map<String, String>> engineList = obj.get("MySql");
//                engine_label = getCustomLabel(engineList, engine);
//            }
        }

        String engine_label = engineOriginal;
        if (engine_label.startsWith("presto")) {
            engine_label = "Ares";
        }
        Meta engineInfo = metaMapper.listByKey(engineOriginal);
        if (null != engineInfo) {
            engine_label = engineInfo.getEngineName();  // 获取引擎标签
        }
        log.info(String.format("%s engine is %s when executing query[uuid=%s]", user, engine_label, uuid));

        if (!engine.startsWith("spark")) {
            List<Account> account = accountMapper.listAll();
//            JSONObject groupAccount = CommonUtil.getUserGroup(user, account, adminUsername, adminPassword);
//            group = groupAccount.getString("group");
//            username = groupAccount.getString("username");
//            password = groupAccount.getString("password");
            for (int i = 0; i < account.size(); i++) {
                if (account.get(i).getUserGroup().equals("BDP")) {
                    username = account.get(i).getUsername();
                    password = account.get(i).getPassword();
                    break;
                }
            }
        }

//        if (group.equals("")) {
//            String message = String.format("%s is not assigned a group[%s]", user, group);
//            queryHistory.setStatusZh(message);
//            response.put("code", 12);
//            response.put("message", String.format("%s, %s", message, "please contact the administrator!"));
//            log.error(String.format("%s when executing query[uuid=%s]: %s", message, uuid, response.toString()));  //todo: del response
//            return response;
//        }

        properties.setProperty("user", username);
        if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
            url = huaweiUrl;
        } else {
            if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
                url = awsSGUrl;
            } else if (!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
                url = olapUrl.getUrl().get(region);
            } else {
                url = awsUrl;
            }
            boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(region));
            if (use_ssl) {
                properties.setProperty("password", password);
            }
            properties.setProperty("SSL", olapUrl.getSslProperty().get(region));
        }

        if (engine.startsWith("presto") && !database.isEmpty()) {
            url = url.replace("default", database);
        }

//        queryHistory.setUserGroup(group);
        queryHistory.setStatus(3);
        queryHistory.setStatusZh("运行中");
        queryHistory.setEngineLabel(engine_label);

        ResultSet rs = null;
        long peakMemoryBytes = 0;
        long cpuTimeMillis = 0;
        long wallTimeMillis = 0;
        long elapsedTimeMillis = 0;
        long queuedTimeMillis = 0;
        long processedRows = 0;
        long processedBytes = 0;

        int id = 0;
        int code = -1;
        int errData = 0;
        log.info("engine是：" + engine);
        boolean b = engine.startsWith("presto") || engine.startsWith("ares") || engine.startsWith("smart");
        try {
            log.info(String.format("[user=%s]store original info to DB when executing query[id=%d, uuid=%s]",
                    user, id, uuid));

            save(queryHistory);
            id = queryHistory.getId();
            String id_conf = String.format("--conf bdp-query-client-id=%d\n", id);
            sql = id_conf + sql;

            log.info(String.format("[user=%s]start to connect url[%s] when executing query[id=%d, uuid=%s]",
                    user, url, id, uuid));
            connection = DriverManager.getConnection(url, properties);
            log.info(String.format("[user=%s] connect success url[%s] when executing query[id=%d, uuid=%s]",
                    user, url, id, uuid));
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);
            log.info(String.format("%s start to retrieve data when executing query[id=%d, uuid=%s]",
                    user, id, uuid));
            queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得
            queryHistory.setQueryId(queryId);
            update(queryHistory);
            log.info(String.format("%s start to get query id when executing query[id=%d, uuid=%s, queryId=%s]",
                    user, id, uuid, queryId));

            HashMap<String, Object> data = new HashMap<>();
            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<String> column = (ArrayList<String>) columns.get("column");
            ArrayList<Map<String, String>> types = (ArrayList<Map<String, String>>) columns.get("type");

            Integer resultLen = 0;
            ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            Integer isAsync = getMysqlAsync(engine);
            log.info(String.format("%s start to get result when executing query[id=%d, uuid=%s]",
                    user, id, uuid));
            log.info("到这一步了597");
            log.info("engine是：" + engine);
            if (engine.startsWith("mysql") && isAsync == 0) {
                result = getResult(rs, column, types, false);
            } else {
                result = getResult(rs, column, types, false);
                resultLen = result.size();
            }
            log.info("到这一步了604");
            log.info(String.format("%s get result success when executing query[id=%d, uuid=%s]",
                    user, id, uuid));

            String domain = getDomain(url, engine);
            log.info(String.format("%s start to query filesize[result length=%d] when executing query[id=%d, uuid=%s]",
                    user, resultLen, id, uuid));
            String size = filesize(domain + "v1/filesize/" + queryId);
            log.info(String.format("%s query filesize sucessfully[result length=%d] when executing query[id=%d, uuid=%s]",
                    user, resultLen, id, uuid));

            elapsedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getElapsedTimeMillis();
            String end = TimeUtil.getNow();
            float executeDuration = elapsedTimeMillis > 0 ? TimeUtil.getTimeFloat(elapsedTimeMillis) / 1000 : TimeUtil.getTimeDiff(start, end);
            BigDecimal bCute = new BigDecimal(executeDuration);
            executeDuration = bCute.setScale(2, RoundingMode.UP).floatValue();

            queryHistory.setStatus(0);
            queryHistory.setStatusZh("已完成");
            queryHistory.setExecuteDuration(executeDuration);
            queryHistory.setColumnType(JSONObject.toJSONString(columns.get("type")));
            queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得
            queryHistory.setQueryId(queryId);
            update(queryHistory);
            log.info(String.format("%s start to get a new query id when executing query[id=%d, uuid=%s, queryId=%s]",
                    user, id, uuid, queryId));

            processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
            log.info(String.format("%s picked processedBytes when executing query[id=%d, uuid=%s]",
                    user, id, uuid));
            String scanSize = b ? FileUtil.getStringSize(processedBytes) : "-";
            if (!engine.startsWith("mysql") || isAsync != 0) {
                if (result.size() == 0 && ((column.size() == 1 && column.get(0).equals("Result")) || column.size() == 0)) {
                    column = new ArrayList<String>();
                    data.put("message", "执行成功");
                    size = "";
                }
                if (result.size() > 0) {
                    JSONObject sample = JSONObject.parseArray(JSONObject.toJSONString(result)).getJSONObject(0);
                    queryHistory.setProbeSample(sample.toJSONString());
                }
            }

            queryHistory.setDataSize(size);
            queryHistory.setScanSize(scanSize);
            queryHistory.setProcessedBytes(processedBytes);
            data.put("meta", column);
            data.put("type", columns.get("type"));
            data.put("repeat_meta", columns.get("repeat"));
            data.put("queryId", queryId);
            data.put("id", id);
            data.put("status", 0);
            data.put("result", result);
            data.put("fileSize", size);
            data.put("scanSize", scanSize);
            data.put("executeDuration", executeDuration);
            data.put("sql", sql);
            response.put("data", data);
            response.put("code", 0);
            update(queryHistory);
            log.info(String.format("%s execute the query[id=%d, uuid=%s] successfully[queryId=%s]",
                    user, id, uuid, queryId));
            saveQueryResult(id, result, columns, tenantName);
        } catch (Exception e) {
            code = 13;
            log.error(String.format("There is a stack err when %s executing query[id=%d, uuid=%s, errCode=%d]: %s",
                    user, id, uuid, code, CommonUtil.printStackTraceToString(e)));

            try {
                throw e;
            } catch (SQLException | ParseException | DataIntegrityViolationException ex) {
                String message = "";
                if (ex.getClass() == SQLException.class) {
                    code = ((SQLException) ex).getErrorCode();
                    message = ex.getMessage().trim();
                    log.error(String.format("There is a SQLException when %s executing query[id=%d, uuid=%s, originalCode=%d]: %s", user, id, uuid, code, message));
                    if (code == 0 && message.equals("Error executing query")) {
                        code = 15;
                        message = String.format("failed to retrieve data: %s", retrieveSQLExceptionCause((SQLException) ex));
                        log.error(String.format("%s failed to retrieve data when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, message));
                    }
                    if ((code == 0 && message.startsWith("Query has no columns") && engine.startsWith("spark"))
                            || (code == 3 && message.endsWith("Query was canceled"))  // presto
                            || (code == 38 && message.contains("Query killed"))) {
                        HashMap<String, Object> data = new HashMap<>();
                        code = 2;
                        String newMessage = "已取消";
                        data.put("status", code);
                        data.put("message", newMessage);
                        queryHistory.setStatus(code);
                        queryHistory.setStatusZh(newMessage);
                        response.put("code", code);
                        response.put("data", data);
                        log.error(String.format("%s cancelled task when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
                        return response;
                    }
                    if (code == 403) {
                        message = String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message);
                        errData = 403;
                        log.error(String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
                    }
                    if (code == 65543 && message.contains("Compiler failed")) {
                        message = ((SQLException) ex).getCause().getCause().toString();
                    }
                    if (code == 0 && message.contains("SQL statement is not a query") && message.toLowerCase().contains("create table") && message.toLowerCase().contains("select")) {
                        code = 55;
                        log.error(String.format("%s is creating table using 'create table XX as'[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
                    }
                    if (code == 0 && message.contains("SQL statement is not a query") && message.toLowerCase().contains("insert into") && message.toLowerCase().contains("select")) {
                        code = 56;
                        log.error(String.format("%s is insert into table using 'insert into XX'[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
                    }
                    if (1001 == code) {
                        String smart_engine = "spark-submit-sql-3_aws_us-east-1";
                        if (region.equalsIgnoreCase("aws_sg")) {
                            smart_engine = "spark-submit-sql-3_aws_ap-southeast-1";
                        } else if (region.equalsIgnoreCase("huawei_sg")) {
                            smart_engine = "spark-submit-sql-3_huawei_ap-southeast-3";
                        }
                        message = String.format("you have smart engine when executing query[id=%d, uuid=%s, errCode=%d]: %s", id, uuid, code, smart_engine);
                        errData = 1001;
                        log.error(String.format("%s has smart engine when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, smart_engine));
                    }
                } else if (ex.getClass() == DataIntegrityViolationException.class) {
                    code = 1406;
                    message = ex.getCause().toString().trim();
                    log.error(String.format("%s failed to save sql when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, ((DataIntegrityViolationException) ex).getMessage()));
                    queryHistory.setQuerySql("");
                    save(queryHistory);
                } else {
                    message = ex.getMessage().trim();
                }
                log.error(String.format("%s failed to query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, message));
                message = String.format("[taskID=%d]%s", id, message);

                float executeDuration = TimeUtil.getTimeDiff(start, TimeUtil.getNow());
                BigDecimal bCute = new BigDecimal(executeDuration);
                executeDuration = bCute.setScale(2, RoundingMode.UP).floatValue();
                code = code > 0 ? code : 999;
                if (code != 55 && code != 56) {
                    response.put("code", code);
                    response.put("message", message);
                    response.put("executeDuration", executeDuration);
                    response.put("data", errData);

                    if (1001 == code) {
                        queryHistory.setStatus(4);
                    } else {
                        queryHistory.setStatus(1);
                    }
                    queryHistory.setStatusZh("已失败: " + message);
                    queryHistory.setExecuteDuration(executeDuration);
                    log.error(String.format("%s failed to query[id=%d, uuid=%s, code=%d], the result returned to the front end is: %s", user, id, uuid, code, response.toString()));
                } else if (code == 55) {
                    queryHistory.setStatus(0);
                    queryHistory.setStatusZh("已完成");
                    queryHistory.setExecuteDuration(executeDuration);
                    update(queryHistory);
                    HashMap<String, Object> data = new HashMap<>();
                    ArrayList<String> column = new ArrayList<String>();
                    ArrayList<String> type = new ArrayList<String>();
                    HashMap<String, Object> repeat_meta = new HashMap<>();
                    ArrayList<String> result = new ArrayList<String>();
                    data.put("meta", column);
                    data.put("type", type);
                    data.put("repeat_meta", repeat_meta);
                    data.put("queryId", queryId);
                    data.put("id", id);
                    data.put("status", 0);
                    data.put("result", result);
                    data.put("fileSize", "");
                    data.put("scanSize", "-");
                    data.put("executeDuration", executeDuration);
                    data.put("message", "创建成功");
                    response.put("data", data);
                    response.put("code", 0);
                    log.info(String.format("%s create table[id=%d, uuid=%s] successfully[queryId=%s]", user, id, uuid, queryId));
                } else {
                    queryHistory.setStatus(0);
                    queryHistory.setStatusZh("已完成");
                    queryHistory.setExecuteDuration(executeDuration);
                    update(queryHistory);
                    HashMap<String, Object> data = new HashMap<>();
                    ArrayList<String> column = new ArrayList<String>();
                    ArrayList<String> type = new ArrayList<String>();
                    HashMap<String, Object> repeat_meta = new HashMap<>();
                    ArrayList<String> result = new ArrayList<String>();
                    data.put("meta", column);
                    data.put("type", type);
                    data.put("repeat_meta", repeat_meta);
                    data.put("queryId", queryId);
                    data.put("id", id);
                    data.put("status", 0);
                    data.put("result", result);
                    data.put("fileSize", "");
                    data.put("scanSize", "-");
                    data.put("executeDuration", executeDuration);
                    data.put("message", "执行成功");
                    response.put("data", data);
                    response.put("code", 0);
                    log.info(String.format("%s create table[id=%d, uuid=%s] successfully[queryId=%s]", user, id, uuid, queryId));
                }
//                return response;
            }
        } finally {
            String scanSize = "-";
            Integer isAsync = getMysqlAsync(engine);
            if (engine.startsWith("mysql") && isAsync == 0) {
                queryHistory.setMysqlAsync(0);
                update(queryHistory);
            }
            if (rs != null) {
                peakMemoryBytes = rs.unwrap(PrestoResultSet.class).getStats().getPeakMemoryBytes();
                cpuTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getCpuTimeMillis();
                wallTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getWallTimeMillis();
                elapsedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getElapsedTimeMillis();
                queuedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getQueuedTimeMillis();
                processedRows = rs.unwrap(PrestoResultSet.class).getStats().getProcessedRows();
                queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得
                if (code != -1) {
                    processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
                    queryHistory.setProcessedBytes(processedBytes);
                    scanSize = b ? FileUtil.getStringSize(processedBytes) : "-";
                    queryHistory.setScanSize(scanSize);
                }
            }

            queryHistory.setPeakMemoryBytes(peakMemoryBytes);
            queryHistory.setCpuTimeMillis(cpuTimeMillis);
            queryHistory.setWallTimeMillis(wallTimeMillis);
            queryHistory.setElapsedTimeMillis(elapsedTimeMillis);
            queryHistory.setQueuedTimeMillis(queuedTimeMillis);
            queryHistory.setProcessedRows(processedRows);
            queryHistory.setQueryId(queryId);
            update(queryHistory);
            log.info(String.format("%s start to get a new query id when query is finished[id=%d, uuid=%s, queryId=%s, peakMemoryBytes=%s, cpuTimeMillis=%s, processedBytes=%s, scanSize=%s]", user, id, uuid, queryId, peakMemoryBytes, cpuTimeMillis, processedBytes, scanSize));

            try {
                if (rs != null) {
                    rs.close();
                }

                QueryHistory temp = queryHistoryMapper.selectByUuid(uuid);
                if (temp.getStatus() != 2) {
                    update(queryHistory);
                } else {
                    HashMap<String, Object> data = new HashMap<>();
                    data.put("status", 2);
                    data.put("message", "已取消");
                    response.put("code", 0);
                    response.put("data", data);
                }
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (Exception ex) {
                code = 14;
                String message = ex.getMessage().trim();
                log.error(String.format("%s failed to close[id=%d, uuid=%s, queryId=%s, errCode=%d] when querying: %s", user, id, uuid, queryId, code, CommonUtil.printStackTraceToString(ex)));
                response.put("code", code);
                response.put("message", message);
                response.put("data", errData);
            }
        }

        if (0 != code && null != response.get("message")) {
            HashMap<String, String> detailErr = getErrorInfo(response.get("message").toString());
            response.put("errorType", detailErr.get("errorType"));
            response.put("errorZh", detailErr.get("errorZh"));
        }
        return response;
    }

    @Override
    public Integer getMysqlAsync(String engine) {
        Integer isAsync = null;

        Meta engineInfo = metaMapper.listByKey(engine);
        if (engineInfo != null) {
            if (engineInfo.getIsAsync() != null) {
                if (engineInfo.getIsAsync() == 0) {
                    isAsync = 0;
                } else {
                    isAsync = 1;
                }
            }
        }

        return isAsync;
    }

    @Override
    @Async
    public void executeMysqlAsyn(String uuid, String engine, String querySql,
                                 String querySqlParam, JSONObject param, String user,
                                 String region, String catalog,
                                 String groupId, int tenantId, String tenantName) throws ParseException, SQLException {
        InfTraceContextHolder.get().setTenantName(tenantName);
        HashMap<String, Object> response = execute(uuid, engine, querySql, 1, 1, user, region, catalog, "",
                groupId, tenantId, tenantName, null);
//        HashMap<String, Object> response = new HashMap<>();
//
//        String start = TimeUtil.getNow();
//        log.info(String.format("%s start to execute query[uuid=%s, engine=%s]", user, uuid, engine));
//        QueryHistory queryHistory = new QueryHistory();
//
//        queryHistory.setCreateBy(user);
//        queryHistory.setUpdateBy(user);
//        queryHistory.setEngine(engine);
//        queryHistory.setQuerySql(querySql);
//        queryHistory.setStartTime(Timestamp.valueOf(start));
//
//        queryHistory.setUuid(uuid);
//        if (checkUuid(queryHistory) == 11){
//            response.put("code", 11);
//            response.put("message", "uuid is not unique");
//        }
//
//        String url = "";
//        String queryId = "";
//        String delimeter = "_";
//        String sql = querySql;
//        Statement statement = null;
//        Connection connection = null;
//        Properties properties = new Properties();
//        String[] engineConfig = engine.split(delimeter);
//
//        String username = "";
//        String password = "";
//
//        log.info(String.format("%s start to get config when executing query[uuid=%s]", user, uuid));
//        String conf = String.format("--conf bdp-query-request-id=%s\n--conf bdp-query-user=%s\n", uuid, user);
//        if (engine.startsWith("smart")) {
//            sql = conf + "--conf olap-engine-mode=smart\n" + sql;
//        } else if (engine.startsWith("presto") || engine.startsWith("spark")) {
//            if (engine.startsWith("spark")) {
//                if(!engine.equals("spark-submit-sql-3_aws_us-east-1")) {  //非spark美东的，不进行路由跳转到awsUrl
//                    conf += "--conf olap-engine-routing=FALSE\n";
//                }
//                List<Account> account = accountMapper.listAll();
//
//                for (int i = 0; i < account.size(); i++) {
//                    if (account.get(i).getUserGroup().equals("spark")) {
//                        username = account.get(i).getUsername();
//                        password = account.get(i).getPassword();
//                        break;
//                    }
//                }
//
//                conf += String.format(
////                        "--conf bdp-query-group=%s\n" +
//                        "--conf bdp-query-engine=%s\n" +
//                                "--conf bdp-query-provider=%s\n" +
//                                "--conf bdp-query-region=%s\n", engineConfig[0], engineConfig[1], engineConfig[2]
//                );
//                sql = conf + querySql;
//            } else {
//                sql = conf + sql;
//            }
//        } else {
//            sql = conf + String.format("--conf bdp-query-engine=%s\n%s", engine, querySql);
//        }
//
//        Meta engineInfo = metaMapper.listByKey(engine);
//        String engine_label = engineInfo.getEngineName();  // 获取引擎标签
//
//        if (!engine.startsWith("spark")) {
//            List<Account> account = accountMapper.listAll();
//            for (int i = 0; i < account.size(); i++) {
//                if (account.get(i).getUserGroup().equals("BDP")) {
//                    username = account.get(i).getUsername();
//                    password = account.get(i).getPassword();
//                    break;
//                }
//            }
//        }
//
//        properties.setProperty("user", username);
//        if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
//            url = huaweiUrl;
//        } else {
//            if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
//                url = awsSGUrl;
//            } else {
//                url = awsUrl;
//            }
//            properties.setProperty("password", password);
//            properties.setProperty("SSL", "true");
//        }
//
//        queryHistory.setStatus(3);
//        queryHistory.setStatusZh("运行中");
//        queryHistory.setEngineLabel(engine_label);
//
//        ResultSet rs = null;
//        long peakMemoryBytes = 0;
//        long cpuTimeMillis = 0;
//        long wallTimeMillis = 0;
//        long elapsedTimeMillis = 0;
//        long queuedTimeMillis = 0;
//        long processedRows = 0;
//        long processedBytes = 0;
//
//        int id = 0;
//        int code = -1;
//        int errData = 0;
//        boolean b = engine.startsWith("presto") || engine.startsWith("ares") || engine.startsWith("smart");
//        try {
//            save(queryHistory);
//            id = queryHistory.getId();
//            log.info(String.format("[user=%s]store original info to DB when executing query[id=%d, uuid=%s]", user, id, uuid));
//
//            log.info(String.format("[user=%s]start to connect url[%s] when executing query[id=%d, uuid=%s]", user, url, id, uuid));
//            connection = DriverManager.getConnection(url, properties);
//            statement = connection.createStatement();
//            rs = statement.executeQuery(sql);
//            log.info(String.format("%s start to retrieve data when executing query[id=%d, uuid=%s]", user, id, uuid));
//            queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得
//            queryHistory.setQueryId(queryId);
//            update(queryHistory);
//
//            HashMap<String, Object> data = new HashMap<>();
//            JSONObject columns = RsUtil.getColumns(rs);
//            ArrayList<String> column = (ArrayList<String>) columns.get("column");
////            ArrayList<Map<String, Object>> result = null;
//            ArrayList<Map<String, Object>> result = getMysqlResult(rs, column, false);
//
//            String domain = getDomain(url, engine);
//            log.info(String.format("%s start to query filesize[result length=%d] when executing query[id=%d, uuid=%s]", user, 0, id, uuid));
//            String size = filesize(domain + "v1/filesize/" + queryId);
//
//            elapsedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getElapsedTimeMillis();
//            String end = TimeUtil.getNow();
//            float executeDuration = elapsedTimeMillis > 0? TimeUtil.getTimeFloat(elapsedTimeMillis)/1000: TimeUtil.getTimeDiff(start, end);
//
//            queryHistory.setStatus(0);
//            queryHistory.setStatusZh("已完成");
//            queryHistory.setExecuteDuration(executeDuration);
//            queryHistory.setColumnType(JSONObject.toJSONString(columns.get("type")));
//
//            processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
//            String scanSize = b ? FileUtil.getStringSize(processedBytes): "-";
////            if (result.size()==0 && ((column.size()==1 && column.get(0).equals("Result")) || column.size()==0)){
////                column = new ArrayList<String>();
////                data.put("message", "执行成功");
////                size = "";
////            }
////            if (result.size() > 0) {
////                JSONObject sample = JSONObject.parseArray(JSONObject.toJSONString(result)).getJSONObject(0);
////                queryHistory.setProbeSample(sample.toJSONString());
////            }
//            queryHistory.setDataSize(size);
//            queryHistory.setScanSize(scanSize);
//            queryHistory.setProcessedBytes(processedBytes);
//            update(queryHistory);
//            data.put("meta", column);
//            data.put("type", columns.get("type"));
//            data.put("repeat_meta", columns.get("repeat"));
//            data.put("queryId", queryId);
//            data.put("status", 0);
//            data.put("result", result);
//            data.put("fileSize", size);
//            data.put("scanSize", scanSize);
//            data.put("executeDuration", executeDuration);
//            data.put("sql", sql);
//            response.put("data", data);
//            response.put("code", 0);
//            log.info(String.format("%s execute the query[id=%d, uuid=%s] successfully[queryId=%s]", user, id, uuid, queryId));
////            HashMap<String, Object> info = (HashMap<String, Object>) data.clone();  // todo: del info
////            info.remove("result");
////            log.info(String.format("%s execute the query[id=%d, uuid=%s] successfully[queryId=%s]: %s", user, id, uuid, queryId, info.toString()));
//        } catch (Exception e) {
//            code = 13;
//            log.error(String.format("There is a stack err when %s executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, CommonUtil.printStackTraceToString(e)));
//
//            try {
//                throw e;
//            } catch (SQLException | ParseException | DataIntegrityViolationException ex) {
//                String message = "";
//                if (ex.getClass() == SQLException.class) {
//                    code = ((SQLException) ex).getErrorCode();
//                    message = ex.getMessage().trim();
//                    log.error(String.format("There is a SQLException when %s executing query[id=%d, uuid=%s, originalCode=%d]: %s", user, id, uuid, code, message));
//                    if (code == 0 && message.equals("Error executing query")) {
//                        code = 15;
//                        message = String.format("failed to retrieve data: %s", retrieveSQLExceptionCause((SQLException) ex));
//                        log.error(String.format("%s failed to retrieve data when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, message));
//                    }
//                    if ((code == 0 && message.startsWith("Query has no columns") && engine.startsWith("spark"))
//                            || (code == 3 && message.endsWith("Query was canceled"))  // presto
//                            || (code == 38 && message.contains("Query killed"))) {
//                        HashMap<String, Object> data = new HashMap<>();
//                        code = 2;
//                        String newMessage = "已取消";
//                        data.put("status", code);
//                        data.put("message", newMessage);
//                        queryHistory.setStatus(code);
//                        queryHistory.setStatusZh(newMessage);
//                        response.put("code", code);
//                        response.put("data", data);
//                        log.error(String.format("%s cancelled task when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
//                    }
//                    if (code == 403) {
//                        message = String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message);
//                        errData = 403;
//                        log.error(String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
//                    }
//                    if (code == 65543 && message.contains("Compiler failed")) {
//                        message = ((SQLException) ex).getCause().getCause().toString();
//                    }
//                    if (code == 0 && message.contains("SQL statement is not a query") && message.toLowerCase().contains("create table") && message.toLowerCase().contains("select")) {
//                        code = 55;
//                        log.error(String.format("%s is creating table using 'create table XX as'[id=%d, uuid=%s, errCode=%d]: %s", user, id, uuid, code, message));
//                    }
//                } else if (ex.getClass() == DataIntegrityViolationException.class) {
//                    code = 1406;
//                    message = ex.getCause().toString().trim();
//                    log.error(String.format("%s failed to save sql when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, ((DataIntegrityViolationException) ex).getMessage()));
//                    queryHistory.setQuerySql("");
//                    save(queryHistory);
//                } else {
//                    message = ex.getMessage().trim();
//                }
//                log.error(String.format("%s failed to query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, id, uuid, queryId, code, message));
//                message = String.format("[taskID=%d]%s", id, message);
//
//                float executeDuration = TimeUtil.getTimeDiff(start, TimeUtil.getNow());
//                code = code>0 ? code: 999;
//                if (code != 55) {
//                    response.put("code", code);
//                    response.put("message", message);
//                    response.put("executeDuration", executeDuration);
//                    response.put("data", errData);
//
//                    queryHistory.setStatus(1);
//                    queryHistory.setStatusZh("已失败: "+message);
//                    queryHistory.setExecuteDuration(executeDuration);
//                    log.error(String.format("%s failed to query[id=%d, uuid=%s, code=%d], the result returned to the front end is: %s", user, id, uuid, code, response.toString()));
//                } else {
//                    queryHistory.setStatus(0);
//                    queryHistory.setStatusZh("已完成");
//                    queryHistory.setExecuteDuration(executeDuration);
//                    update(queryHistory);
//                    HashMap<String, Object> data = new HashMap<>();
//                    ArrayList<String> column = new ArrayList<String>();
//                    ArrayList<String> type = new ArrayList<String>();
//                    HashMap<String, Object> repeat_meta = new HashMap<>();
//                    ArrayList<String> result = new ArrayList<String>();
//                    data.put("meta", column);
//                    data.put("type", type);
//                    data.put("repeat_meta", repeat_meta);
//                    data.put("queryId", queryId);
//                    data.put("status", 0);
//                    data.put("result", result);
//                    data.put("fileSize", "");
//                    data.put("scanSize", "-");
//                    data.put("executeDuration", executeDuration);
//                    data.put("message", "创建成功");
//                    response.put("data", data);
//                    response.put("code", 0);
//                    log.info(String.format("%s create table[id=%d, uuid=%s] successfully[queryId=%s]", user, id, uuid, queryId));
//                }
////                return response;
//            }
//        } finally {
//            update(queryHistory);
//            QueryHistory mysqlQueryHistory = queryHistoryMapper.selectByUuid(uuid);
//            Integer isAsync = getMysqlAsync(engine);
//            if (engine.startsWith("mysql") && isAsync == 0) {
//                mysqlQueryHistory.setMysqlAsync(0);
//                update(mysqlQueryHistory);
//            }
//            if (rs != null) {
//                peakMemoryBytes = rs.unwrap(PrestoResultSet.class).getStats().getPeakMemoryBytes();
//                cpuTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getCpuTimeMillis();
//                wallTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getWallTimeMillis();
//                elapsedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getElapsedTimeMillis();
//                queuedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getQueuedTimeMillis();
//                processedRows = rs.unwrap(PrestoResultSet.class).getStats().getProcessedRows();
//                if (code != -1) {
//                    processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
//                    mysqlQueryHistory.setProcessedBytes(processedBytes);
//                    String scanSize = b ? FileUtil.getStringSize(processedBytes): "-";
//                    mysqlQueryHistory.setScanSize(scanSize);
//                }
//            }
//
//            mysqlQueryHistory.setPeakMemoryBytes(peakMemoryBytes);
//            mysqlQueryHistory.setCpuTimeMillis(cpuTimeMillis);
//            mysqlQueryHistory.setWallTimeMillis(wallTimeMillis);
//            mysqlQueryHistory.setElapsedTimeMillis(elapsedTimeMillis);
//            mysqlQueryHistory.setQueuedTimeMillis(queuedTimeMillis);
//            mysqlQueryHistory.setProcessedRows(processedRows);
//
//            try {
//                if (rs != null) {
//                    rs.close();
//                }
//
//                QueryHistory temp = queryHistoryMapper.selectByUuid(uuid);
//                if (temp.getStatus() != 2) {
//                    update(mysqlQueryHistory);
//                } else {
//                    HashMap<String, Object> data = new HashMap<>();
//                    data.put("status", 2);
//                    data.put("message", "已取消");
//                    response.put("code", 0);
//                    response.put("data", data);
//                }
//                if (statement != null) {
//                    statement.close();
//                    connection.close();
//                }
//            } catch (Exception ex) {
//                code = 14;
//                String message = ex.getMessage().trim();
//                log.error(String.format("%s failed to close[id=%d, uuid=%s, queryId=%s, errCode=%d] when querying: %s", user, id, uuid, queryId, code, CommonUtil.printStackTraceToString(ex)));
//                response.put("code", code);
//                response.put("message", message);
//                response.put("data", errData);
//            }
//        }

        saveQuerySqlParam(uuid, querySqlParam, param);

//        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
//        if (queryHistory != null) {
//            queryHistory.setMysqlAsync(0);
//            update(queryHistory);
//        }
//        if (response.get("code").equals(0) || response.get("code").equals(2)){
//            if (response.get("code").equals(0)) {
//                HashMap<String, Object> data = (HashMap<String, Object>) response.get("data");
//                JSONArray type = JSONObject.parseArray(JSONObject.toJSONString(data.get("type")));
//                JSONArray result = JSONObject.parseArray(JSONObject.toJSONString(data.get("result")));
//                if (result != null) {
//                    if (result.size() > 0) {
//                        JSONObject sample = result.getJSONObject(0);
//                        probeAsyn(uuid, user, type, false, sample);
//                    }
//                }
//            }
//        }
    }

    @Override
    public HashMap<String, Object> executeMysqlSample(String uuid, String engine, String querySql, String user) throws ParseException, SQLException {
        log.info(String.format("user %s starts to execute mysql query to get sample data[uuid=%s, engine=%s]", user, uuid, engine));
        String start = TimeUtil.getNow();

        HashMap<String, Object> response = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        String querySqlSample = "";
        Integer limitNum = displayCount;

        if (querySql.toLowerCase().endsWith(";") || querySql.toLowerCase().endsWith("；")) {
            querySql = querySql.substring(0, querySql.length() - 1);
        }

        if (querySql.toLowerCase().startsWith("select")) {
            log.info(String.format("user %s executes mysql query to get sample data: it's a select sql[uuid=%s, engine=%s]", user, uuid, engine));
            if (querySql.toLowerCase().contains("limit")) {
                log.info(String.format("user %s executes mysql query to get sample data: it has limit[uuid=%s, engine=%s]", user, uuid, engine));
                String querySqlformat = SQLUtils.format(querySql, JdbcConstants.MYSQL, SQLUtils.DEFAULT_LCASE_FORMAT_OPTION);
                ArrayList<String> querySqlList = new ArrayList<String>(Arrays.asList(querySqlformat.split("\n")));
                String querySqlLast = querySqlList.get(querySqlList.size() - 1).trim();
                if (querySqlLast.toLowerCase().startsWith("limit")) {
                    log.info(String.format("user %s executes mysql query to get sample data: limit is at the end[uuid=%s, engine=%s]", user, uuid, engine));
                    ArrayList<String> querySqlLimitList = new ArrayList<String>(Arrays.asList(querySqlLast.split(" ")));
                    Integer queryLimit = Integer.parseInt(querySqlLimitList.get(1));
                    if (queryLimit > limitNum) {
                        log.info(String.format("user %s executes mysql query to get sample data: limit > 1000[uuid=%s, engine=%s]", user, uuid, engine));
                        querySqlLimitList.set(1, limitNum.toString());
                        querySqlList.set(querySqlList.size() - 1, StringUtils.join(querySqlLimitList, " "));
                        querySqlSample = StringUtils.join(querySqlList, "\n");
                    } else {
                        log.info(String.format("user %s executes mysql query to get sample data: limit <= 1000[uuid=%s, engine=%s]", user, uuid, engine));
                        querySqlSample = querySql;
                    }
                } else {
                    log.info(String.format("user %s executes mysql query to get sample data: limit is not at the end[uuid=%s, engine=%s]", user, uuid, engine));
                    querySqlSample = querySql + "\nlimit " + limitNum.toString();
                }
            } else {
                log.info(String.format("user %s executes mysql query to get sample data: it doesn't have limit[uuid=%s, engine=%s]", user, uuid, engine));
                querySqlSample = querySql + "\nlimit " + limitNum.toString();
            }
        } else {
            log.info(String.format("user %s executes mysql query to get sample data: it is not a select sql[uuid=%s, engine=%s]", user, uuid, engine));
            querySqlSample = querySql;
        }

        log.info(String.format("user %s executes mysql query to get sample data: final sql[uuid=%s, engine=%s]: %s", user, uuid, engine, querySqlSample));

        String JdbcDriver = "com.mysql.jdbc.Driver";

        Meta engineInfo = metaMapper.listByKey(engine);
        String url = engineInfo.getEngineUrl();
        String username = engineInfo.getUsername();
        String password = engineInfo.getPassword();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        float executeDuration = 0;

        try {
            Class.forName(JdbcDriver);

            log.info(String.format("user %s executes mysql query to get sample data: connecting to database[uuid=%s, engine=%s]", user, uuid, engine));
            conn = DriverManager.getConnection(url, username, password);

            log.info(String.format("user %s executes mysql query to get sample data: connected to database[uuid=%s, engine=%s]", user, uuid, engine));

            stmt = conn.createStatement();
            rs = stmt.executeQuery(querySqlSample);

            log.info(String.format("user %s executes mysql query to get sample data: executeQuery[uuid=%s, engine=%s]", user, uuid, engine));


            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<String> column = (ArrayList<String>) columns.get("column");
            ArrayList<Map<String, String>> types = (ArrayList<Map<String, String>>) columns.get("type");
            ArrayList<Map<String, Object>> result = getResult(rs, column, types, false);

            log.info(String.format("user %s executes mysql query to get sample data: get results[uuid=%s, engine=%s]", user, uuid, engine));


            rs.close();
            stmt.close();
            conn.close();

            String end = TimeUtil.getNow();
            executeDuration = TimeUtil.getTimeDiff(start, end);

            log.info(String.format("user %s executes mysql query to get sample data successfully[uuid=%s, engine=%s, executeDuration=%s]", user, uuid, engine, executeDuration));

            data.put("meta", column);
            data.put("type", columns.get("type"));
            data.put("repeat_meta", columns.get("repeat"));
            data.put("queryId", "");
            data.put("status", 0);
            data.put("result", result);
            data.put("fileSize", "");
            data.put("scanSize", "-");
            data.put("executeDuration", executeDuration);
            data.put("sql", querySqlSample);
            response.put("data", data);
            response.put("code", 0);
        } catch (Exception se) {
            Integer code = 13;
            log.error(String.format("There is a SQLException when %s execute mysql query to get sample data[uuid=%s]: %s", user, uuid, CommonUtil.printStackTraceToString(se)));

            try {
                throw se;
            } catch (SQLException | ParseException | DataIntegrityViolationException | ClassNotFoundException ex) {
                String message = ex.getMessage().trim();
                log.error(String.format("There is a Exception when %s execute mysql query to get sample data[uuid=%s, originalCode=%d]: %s", user, uuid, code, message));
                response.put("code", code);
                response.put("message", message);
                response.put("executeDuration", executeDuration);
                response.put("data", 0);
            }
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException se1) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se3) {
                se3.printStackTrace();
            }
        }


        return response;
    }

    @Override
    public HashMap<String, Object> getFileSize(String uuid, String user) {
//        log.info(String.format("%s start to get file size after mysql query[uuid=%s]", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        int code = 0;

        if (queryHistory == null) {
            code = 55;
            String message = String.format("There is no vaild uuid in mysql query[uuid=%s]", uuid);
//            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
        } else {
            Integer mysqlAsync = queryHistory.getMysqlAsync();
            if (mysqlAsync == null) {
                String message = String.format("The mysql query[uuid=%s] is not ready", uuid);
//                log.info(String.format("[%s]%s", user, message));
                data.put("ready", 1);
                data.put("fileSize", "-");
                response.put("code", code);
                response.put("data", data);
                response.put("message", message);
            } else {
                String message = String.format("The mysql query[uuid=%s] is ready", uuid);
//                log.info(String.format("[%s]%s", user, message));
                data.put("ready", 0);
                data.put("fileSize", queryHistory.getDataSize());
                response.put("code", code);
                response.put("data", data);
                response.put("message", message);
            }
        }
//        log.info(String.format("%s get file size after mysql query[uuid=%s] successfully", user, uuid));
        return response;
    }

    public void saveQuerySqlParam(String uuid, String querySqlParam, JSONObject param) {
        if (querySqlParam != null && param != null) {
            QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
            log.info(String.format("execute stage-record saveQuerySqlParam get success uuid %s", uuid));
            queryHistory.setQuerySqlParam(querySqlParam);
            queryHistory.setParam(JSONObject.toJSONString(param));
            update(queryHistory);
            log.info(String.format("execute stage-record saveQuerySqlParam update success uuid %s", uuid));
        }
    }

    private String getAuthorization(String user, String password) {
        // 获取经过编码的用户名密码
        String userInfo = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + userInfo;
    }

    @Override
    @Async
    public HashMap<String, Object> cancel(String uuid, String user, String tenantName) throws InterruptedException {
        InfTraceContextHolder.get().setTenantName(tenantName);
        log.info(String.format("%s start to cancel task[uuid=%s]", user, uuid));
        HashMap<String, Object> result = new HashMap<>();
        QueryHistory queryHistory = null;

        int count = 0;
        for (int i = 0; i < cancelCount; i++) {
            queryHistory = queryHistoryMapper.selectByUuid(uuid);
            if (queryHistory == null) {
                count += 1;
                Thread.sleep(cancelSleep);    //延时2秒
            } else {
                break;
            }
        }
        assert queryHistory != null;
        HashMap<String, Object> data = new HashMap<>();
        if (count >= cancelCount) {
            int code = 21;
            String message = "failed to get queryId";
            data.put("status", code);
            data.put("message", String.format("[uuid=%s]%s", uuid, message));
            result.put("code", 0);
            result.put("data", data);
            log.error(String.format("%s %s when cancel task[uuid=%s, code=%d]", user, message, uuid, code));
            return result;
        }

        int id = queryHistory.getId();
        String queryId = queryHistory.getQueryId();
        String engine = queryHistory.getEngine();
//        String group = queryHistory.getUserGroup();
        int status = queryHistory.getStatus();

        String url = "";
        if (status != 3) {
            data.put("status", status);
            data.put("message", String.format("[id=%d]%s", queryHistory.getId(), queryHistory.getStatusZh()));
            result.put("code", 0);
            result.put("data", data);
            log.info(String.format("The task[id=%d, uuid=%s, queryId=%s, status=%d] has stopped running", id, uuid, queryId, status));
            return result;
        }
        // for
        if (engine.startsWith("presto") || engine.startsWith("spark") || engine.startsWith("ares") || engine.startsWith("smart")) {
            String username = "";
            String password = "";

            List<Account> account = accountMapper.listAll();
            if (engine.startsWith("spark")) {
                for (Account value : account) {
                    if (value.getUserGroup().equals("spark")) {
                        username = value.getUsername();
                        password = value.getPassword();
                        break;
                    }
                }
            } else {
                boolean hasAccount = false;
                for (Account value : account) {
                    if (value.getUserGroup().equals("BDP")) {
                        username = value.getUsername();
                        password = value.getPassword();
                        hasAccount = true;
                        break;
                    }
                }
//                if (!hasAccount) {
//                    int code = 24;
//                    String message = String.format("The user does not have a valid group[%s]", group);
//                    data.put("status", code);
//                    data.put("message", message);
//                    result.put("code", 0);
//                    result.put("data", data);
//                    log.error(String.format("[%s]%s when cancel query[uuid=%s, code=%d]", user, message, uuid, code));
//                }
            }

            String userInfo = "";
            if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
                url = huaweiUrl;
                userInfo = getAuthorization(username, "");
            } else {
                if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
                    url = awsSGUrl;
                } else if (!queryHistory.getRegion().equals("aws_ue1")) {
                    url = olapUrl.getUrl().get(queryHistory.getRegion());
                } else {
                    url = awsUrl;
                }
                userInfo = getAuthorization(username, password);
            }

            String domain = getDomain(url, engine);
            String httpUri = domain + "v1/query/request/" + uuid;

            log.info(String.format("[id=%d, user=%s] start to connect url[%s] when cancelling task %s", id, user, url, uuid));
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(httpUri);
            RequestConfig requestConfig = RequestConfig.custom().
                    setConnectTimeout(20000).setConnectionRequestTimeout(20000).setSocketTimeout(30000).build();
            httpDelete.setConfig(requestConfig);
            httpDelete.addHeader("Authorization", userInfo);
            httpDelete.addHeader("Content-Type", "application/json");

            JSONObject jsonObject = new JSONObject();
            httpDelete.setEntity(new StringEntity(JSON.toJSONString(jsonObject), "UTF-8"));

            int code = 23;
            log.info(String.format("%s start to judge status when cancelling task [id=%d, uuid=%s]", user, id, uuid));
            try (CloseableHttpResponse response = client.execute(httpDelete)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // 对已经取消的任务，或已经成功的任务，或不存在的任务，执行kill时，status均为200
                    QueryHistory temp = queryHistoryMapper.selectByUuid(uuid);
                    if (temp.getStatus() == 2) {
                        code = temp.getStatus();
                        data.put("status", 2);
                        data.put("message", "已取消");
                        result.put("code", 0);
                        result.put("data", data);
                        log.error(String.format("%s failed to cancel task, because the task has been cancelled[id=%d, uuid=%s, code=%d]", user, id, uuid, code));
                        return result;
                    }
                    queryHistory.setStatus(2);
                    queryHistory.setStatusZh("已取消");
                    update(queryHistory);
                    data.put("status", 2);
                    data.put("message", "已取消");
                    result.put("code", 0);
                    result.put("data", data);
                    response.close();
                    log.info(String.format("%s cancel task[id=%d, uuid=%s, query_id=%s] successfully", user, id, uuid, queryHistory.getQueryId()));
                } else {
                    code = statusCode > 0 ? statusCode : 22;
                    String message = EntityUtils.toString(response.getEntity()).trim();
                    data.put("status", code);
                    data.put("message", String.format("[id=%d]%s", id, message));
                    result.put("code", 0);
                    result.put("data", data);
                    log.error(String.format("%s failed to cancel task[id=%d, uuid=%s, query_id=%s, errorCode=%d]: %s", user, id, uuid, queryHistory.getQueryId(), code, message));
                }
            } catch (Exception ex) {
                log.error(String.format("[%s] An exception occurred when canceling the task[id=%d, uuid=%s, query_id=%s, errorCode=%d]: %s", user, id, uuid, queryHistory.getQueryId(), code, CommonUtil.printStackTraceToString(ex)));
                String message = ex.getMessage().trim();
                if (message.equals("Read timed out") && engine.startsWith("spark")) {
                    code = 26;
                    queryHistory.setStatus(2);
                    queryHistory.setStatusZh("已取消");
                    update(queryHistory);
                    data.put("status", 2);
                    data.put("message", "已取消");
                    HashMap<String, String> info = new HashMap<>();
                    info.put("id", String.valueOf(id));
                    info.put("url", url);
                    info.put("uuid", uuid);
                    info.put("user", user);
                    info.put("engine", engine);
                    info.put("username", username);
                    info.put("password", password);
                    data.put("info", info);
                    result.put("code", code);
                    result.put("data", data);
                    log.info(String.format("%s cancel task[id=%d, uuid=%s, query_id=%s] time out, which needs to retry", user, id, uuid, queryHistory.getQueryId()));
                } else {
                    result.put("code", code);
                    result.put("message", String.format("[id=%d]%s", id, message));
                }
            } finally {
                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (Exception ex) {
                    code = 25;
                    String message = ex.getMessage().trim();
                    log.error(String.format("%s failed to close client[id=%d, uuid=%s, queryId=%s, errCode=%d] when cancel task: %s", user, id, uuid, queryId, code, CommonUtil.printStackTraceToString(ex)));
                    result.put("code", code);
                    result.put("message", message);
                }
            }
        } else {
            data.put("status", 2);
            data.put("message", "已取消");
            result.put("code", 0);
            result.put("data", data);
        }

        return result;
    }

    @Override
    @Async
    public void cancelAsync(Map<String, String> info, String tenantName) {
        InfTraceContextHolder.get().setTenantName(tenantName);
        log.info(String.format("%s start to cancel task[uuid=%s] asynchronously", info.get("user"), info.get("uuid")));
        String userInfo = "";
        if (info.get("engine").equals("presto_huawei") || info.get("engine").equals("smart_huawei")) {
            userInfo = getAuthorization(info.get("username"), "");
        } else {
            userInfo = getAuthorization(info.get("username"), info.get("password"));
        }
        String domain = getDomain(info.get("url"), info.get("engine"));
        String httpUri = domain + "v1/query/request/" + info.get("uuid");

        for (int i = 0; i < sparkCancelCount; i++) {
            log.info(String.format("[user=%s, id=%s, i=%d] start to connect url[%s] when cancelling task %s asynchronously", info.get("user"), info.get("id"), i, info.get("url"), info.get("uuid")));
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(httpUri);
            RequestConfig requestConfig = RequestConfig.custom().
                    setConnectTimeout(20000).setConnectionRequestTimeout(20000).setSocketTimeout(30000).build();
            httpDelete.setConfig(requestConfig);
            httpDelete.addHeader("Authorization", userInfo);
            httpDelete.addHeader("Content-Type", "application/json");

            JSONObject jsonObject = new JSONObject();
            httpDelete.setEntity(new StringEntity(JSON.toJSONString(jsonObject), "UTF-8"));

            log.info(String.format("%s start to judge status when cancelling task [id=%s, uuid=%s, i=%d] asynchronously", info.get("user"), info.get("id"), info.get("uuid"), i));
            CloseableHttpResponse response = null;
            try {
                response = client.execute(httpDelete);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // 对已经取消的任务，或已经成功的任务，或不存在的任务，执行kill时，status均为200
                    QueryHistory temp = queryHistoryMapper.selectByUuid(info.get("uuid"));
                    if (temp.getStatus() == 2) {
                        log.error(String.format("%s failed to cancel task, because the task has been cancelled[id=%s, uuid=%s, i=%d, code=%d] asynchronously", info.get("user"), info.get("id"), info.get("uuid"), i, statusCode));
                    } else {
                        log.info(String.format("The task has been successfully canceled asynchronously[user=%s, id=%s, uuid=%s, i=%d]", info.get("user"), info.get("id"), info.get("uuid"), i));
                    }
                    break;
                } else {
                    String message = EntityUtils.toString(response.getEntity()).trim();
                    log.error(String.format("The task failed to be canceled asynchronously[user=%s, id=%s, uuid=%s, i=%d, errorCode=%d]: %s", info.get("user"), info.get("id"), info.get("uuid"), i, statusCode, message));
                }
            } catch (Exception ex) {
                log.error(String.format("There is an exception occurred when canceling the task asynchronously[user=%s, id=%s, uuid=%s, i=%d]: %s", info.get("user"), info.get("id"), info.get("uuid"), i, CommonUtil.printStackTraceToString(ex)));
            } finally {
                try {
                    if (client != null) {
                        client.close();
                    }
                    if (response != null) {
                        response.close();
                    }
                } catch (Exception ex) {
                    log.error(String.format("%s failed to close client[id=%s, uuid=%s, i=%d] when canceling task asynchronously: %s", info.get("user"), info.get("id"), info.get("uuid"), i, CommonUtil.printStackTraceToString(ex)));
                }
            }
        }
    }

    private static void writeRow(List<Object> row, BufferedWriter csvWriter) throws IOException {
        // 写入文件
        for (Object data : row) {
            String rowStr = "";
            if (data != null) {
                rowStr = data.toString().trim();
                if (rowStr.contains("\"")) {
                    rowStr = rowStr.replaceAll("\"", "\"\"");
                }
                if (rowStr.startsWith("--")) {
                    rowStr = " " + rowStr;
                }
            }
            if (-1 == rowStr.indexOf(',') && -1 == rowStr.indexOf('\n')) {
                csvWriter.write(rowStr + ",");
            } else {
                csvWriter.write("\"" + rowStr + "\",");
            }
        }
        csvWriter.newLine();
    }

    public void downloadToCsv(String uuid, String user, File csvFile,
                              boolean updateColumnType, QueryHistory queryHistory) throws Exception {
        log.info(String.format("%s start to download task %s", user, uuid));
        String engine = queryHistory.getEngine();
        Integer isAsync = getMysqlAsync(engine);
        if (engine.startsWith("mysql") && isAsync == 0) {
            queryHistory = queryHistoryMapper.selectByUuid(uuid);
            Integer mysqlAsync = queryHistory.getMysqlAsync();
            String start = TimeUtil.getNow();
            String end = TimeUtil.getNow();
            while (mysqlAsync == null && TimeUtil.getTimeDiff(start, end) < 600) {
//                log.info(String.format("Mysql query [uuid=%s] is not ready, waiting ......", uuid));
                Thread.sleep(2000);
                end = TimeUtil.getNow();
                queryHistory = queryHistoryMapper.selectByUuid(uuid);
                mysqlAsync = queryHistory.getMysqlAsync();
            }
        }

        String queryId = queryHistory.getQueryId();
        String sql = queryHistory.getQuerySql();

        String url = "";
        String querySql = "";
        String conf = String.format("--conf bdp-query-user=%s\n--conf bdp-query-id=%s\n", user, queryId);
        if (engine.startsWith("smart")) {
            querySql = conf + "--conf olap-engine-mode=smart\n" + sql;
        } else {
            querySql = conf + sql;
        }

        String region = queryHistory.getRegion();
        if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
            url = huaweiUrl;
        } else {
            if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
                url = awsSGUrl;
            } else if (!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
                url = olapUrl.getUrl().get(queryHistory.getRegion());
            } else {
                url = awsUrl;
            }
        }

        Properties properties = new Properties();
        try {
            List<Account> account = accountMapper.listAll();
//            JSONObject groupAccount = CommonUtil.getUserGroup(user, account, adminUsername, adminPassword);
            String username = "";
            String password = "";
            for (int i = 0; i < account.size(); i++) {
                if (account.get(i).getUserGroup().equals("BDP")) {
                    username = account.get(i).getUsername();
                    password = account.get(i).getPassword();
                    break;
                }
            }
            properties.setProperty("user", username);
            boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(queryHistory.getRegion()));
            if (!engine.equals("presto_huawei") && !engine.equals("smart_huawei")) {
                if (use_ssl) {
                    properties.setProperty("password", password);
                }
                properties.setProperty("SSL", olapUrl.getSslProperty().get(queryHistory.getRegion()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            String message = "";
            if (ex.getClass() == java.lang.NullPointerException.class) {
                message = "failed to get user group";
            } else {
                message = ex.getMessage().trim();
                if (message == null) {
                    message = ex.toString();
                }
            }
            log.error(String.format("%s failed to getUserGroup[%s] to server when downloading: %s", user, uuid, message));
            throw new Exception(message);
        }

        ResultSet rs = null;
        Statement statement = null;
        Connection connection = null;
        BufferedWriter csvWriter = null;

        try {
            log.info(String.format("%s start to connect url[%s] when downloading %s", user, url, uuid));
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(querySql);

            log.info(String.format("%s start to generate empty file when downloading %s", user, uuid));
//            File csvFile = new File(fileDir + File.separator + uuid + ".csv");
            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (csvFile.exists()) {
                csvFile.delete();
            }
            csvFile.createNewFile();

            log.info(String.format("%s start to write file when downloading %s", user, uuid));
            // GB2312使正确读取分隔符","
            csvWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    csvFile), StandardCharsets.UTF_8), 1024);
            csvWriter.write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8));

            List<Object> column = new ArrayList<>();
            ArrayList<Map<String, String>> type = new ArrayList<>();
            String fileName = csvFile.getName();
            Integer duplicateCount = 0;
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
//                String columnName = rs.getMetaData().getColumnName(i+1);
//                if (fileName.contains("_probe") && column.contains(columnName)) {
//                    duplicateCount = duplicateCount + 1;
//                    column.add(columnName + duplicateCount.toString());
//                } else {
//                    column.add(columnName);
//                }
                if (updateColumnType) {
                    Map<String, String> column_type = new HashMap<>();
                    column_type.put(rs.getMetaData().getColumnName(i + 1), ProbeConstant.columnTypeMap.get(rs.getMetaData().getColumnType(i + 1)));
                    type.add(column_type);
                }
            }
            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<Map<String, String>> types = (ArrayList<Map<String, String>>) columns.get("type");
            for (int j = 0; j < types.size(); j++) {
                Set keySet = types.get(j).keySet();
                for (Object keyName : keySet) {
                    column.add(keyName);
                }
            }
            writeRow(column, csvWriter);
            if (updateColumnType) {
                queryHistory.setColumnType(JSONObject.toJSONString(type));
                update(queryHistory);
                log.info(String.format("%s update columnType when probe[uuid=%s] asynchronously", user, uuid));
            }

            int count = 0;
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < column.size(); i++) {
                    row.add(rs.getString(i + 1));
                }
                writeRow(row, csvWriter);
                count += 1;
                if (count >= downloadCount) {
                    break;
                }
            }
            csvWriter.flush();
            log.info(String.format("%s write file[%s] to server successfully when downloading", user, uuid));
        } catch (Exception ex) {
            log.error(String.format("There is a stack error, which %s write file[%s] to server when downloading: %s", user, uuid, CommonUtil.printStackTraceToString(ex)));
            String message = ex.getMessage().trim();
            throw new Exception(message);
        } finally {
            try {
                if (null != csvWriter) {
                    csvWriter.close();
                }
                assert rs != null;
                rs.close();
                statement.close();
                connection.close();
            } catch (Exception ex) {
                log.error(String.format("%s failed to close [%s] file or connection when downloading: %s", user, uuid, CommonUtil.printStackTraceToString(ex)));
            }
        }
    }

    @Override
    public ResponseEntity<Object> download(String uuid, String user) throws Exception {
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            String message = String.format("There is no uuid[%s] info when downloading", uuid);
            log.error(String.format("[%s]%s", user, message));
            throw new Exception(message);
        }

        String root = System.getProperty("user.dir");
        String fileDir = root + File.separator + "data" + File.separator + "csv";
        log.info(String.format("%s start to write file to web when download %s", user, uuid));
        File file = new File(fileDir + File.separator + String.valueOf(queryHistory.getId()) + ".csv");

        downloadToCsv(uuid, user, file, false, queryHistory);

        FileInputStream inputStream = new FileInputStream(file);
        InputStreamResource resource = new InputStreamResource(inputStream);
        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Type", "application/csv; charset=UTF-8");
        headers.add("Content-Disposition", String.format("attachment;filename=\"%s", URLEncoder.encode(file.getName(), String.valueOf(StandardCharsets.UTF_8))));
        headers.add("Cache-Control", "no-cache,no-store,must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Access-Control-Expose-Headers", "Content-Disposition");

        ResponseEntity.BodyBuilder ok = ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/csv"));
//                .contentType(MediaType.parseMediaType("application/x-java-archive"));

        return ok.body(resource);
    }

    @Override
    public String downloadToNative(String uuid, String user) throws Exception {
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            String message = String.format("There is no uuid[%s] info when downloading", uuid);
            log.error(String.format("[%s]%s", user, message));
            throw new Exception(message);
        }

        String root = System.getProperty("user.dir");
        String fileDir = root + File.separator + "data" + File.separator + "csv";
        log.info(String.format("%s start to write file to web when download %s", user, uuid));
        String fileName = fileDir + File.separator + String.valueOf(queryHistory.getId()) + "_temp.csv";
        File file = new File(fileName);

        downloadToCsv(uuid, user, file, false, queryHistory);
        return fileName;
    }

    @Override
    public HashMap<String, Object> historySql(String uuid, String user) throws Exception {
        log.info(String.format("%s start to query historical sql[uuid=%s]", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null) {
            int code = 31;
            String message = String.format("There is no valid uuid[%s] when query historical sql", uuid);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("status", queryHistory.getStatus());
        data.put("querySql", queryHistory.getQuerySql());
        data.put("querySqlParam", queryHistory.getQuerySqlParam());
        data.put("param", JSONObject.parseObject(queryHistory.getParam()));
        data.put("engine", queryHistory.getEngine());
        data.put("queryId", queryHistory.getQueryId());
        data.put("region", queryHistory.getRegion());
        data.put("catalog", queryHistory.getCatalog());

        if (queryHistory.getStatus() != 0) {
            data.put("message", queryHistory.getStatusZh());
            data.put("codeStr", "SYS_ERR");
        }

        response.put("code", 0);
        response.put("data", data);
        return response;
    }

    @Override
    public HashMap<String, Object> historyData(String uuid, String user) throws Exception {
        log.info(String.format("%s start to query historical data[uuid=%s]", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null) {
            int code = 32;
            String message = String.format("There is no valid uuid[%s] when query historical data", uuid);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        if (queryHistory.getStatus() != 0) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("status", queryHistory.getStatus());
            data.put("message", queryHistory.getStatusZh());
            data.put("executeDuration", queryHistory.getExecuteDuration());
            if (queryHistory.getStatus() == 3) {
                data.put("engine", queryHistory.getEngine());
                data.put("region", queryHistory.getRegion());
                data.put("catalog", queryHistory.getCatalog());
            }
            data.put("codeStr", "SYS_ERR");
            HashMap<String, String> detailErr = getErrorInfo(queryHistory.getStatusZh());
            data.put("errorType", detailErr.get("errorType"));
            data.put("errorZh", detailErr.get("errorZh"));
            response.put("code", 0);
            response.put("data", data);
            return response;
        }

        String username = "";
        String password = "";

        List<Account> account = accountMapper.listAll();
        if (queryHistory.getEngine().startsWith("spark")) {
            for (Account value : account) {
                if (value.getUserGroup().equals("spark")) {
                    username = value.getUsername();
                    password = value.getPassword();
                    break;
                }
            }
        } else {
            boolean hasAccount = false;
            for (Account value : account) {
                if (value.getUserGroup().equals("BDP")) {
                    username = value.getUsername();
                    password = value.getPassword();
                    hasAccount = true;
                    break;
                }
            }
//            if (!hasAccount) {
//                int code = 33;
//                String message = String.format("[user=%s, id=%d] doesn't have a valid group[%s] when historical query data", user, queryHistory.getId(), queryHistory.getUserGroup());
//                response.put("code", code);
//                response.put("message", message);
//                log.error(String.format("%s when historical query data", message));
//            }
        }

        String url = "";
        String sql = "";
        String querySql = queryHistory.getQuerySql();
        String engine = queryHistory.getEngine();
        String conf = String.format("--conf bdp-query-user=%s\n--conf bdp-query-id=%s\n", user, queryHistory.getQueryId());

        if (queryHistory.getStatus() == 0 && queryHistory.getQuerySql().toLowerCase().contains("create table") && queryHistory.getQuerySql().toLowerCase().contains("select")) {
            HashMap<String, Object> data = new HashMap<>();
            ArrayList<String> column = new ArrayList<String>();
            HashMap<String, Object> repeat_meta = new HashMap<>();
            ArrayList<String> result = new ArrayList<String>();
            data.put("status", 0);
            data.put("uuid", uuid);
            data.put("meta", column);
            data.put("repeat_meta", repeat_meta);
            data.put("result", result);
            data.put("engine", queryHistory.getEngine());
            data.put("queryId", queryHistory.getQueryId());
            data.put("id", queryHistory.getId());
            data.put("querySql", querySql);
            data.put("querySqlParam", queryHistory.getQuerySqlParam());
            data.put("param", JSONObject.parseObject(queryHistory.getParam()));
            data.put("fileSize", queryHistory.getDataSize());
            data.put("scanSize", queryHistory.getEngine().startsWith("presto") ? FileUtil.getStringSize(queryHistory.getProcessedBytes()) : "-");
            data.put("executeDuration", queryHistory.getExecuteDuration() == null ? 0 : queryHistory.getExecuteDuration());
            data.put("message", "创建成功");
            data.put("region", queryHistory.getRegion());
            data.put("catalog", queryHistory.getCatalog());
            data.put("type", JSONArray.parseArray(queryHistory.getColumnType()));
            response.put("data", data);
            response.put("code", 0);
            log.info(String.format("%s execute the historical query data that is creating table as[id=%d, uuid=%s] successfully[queryId=%s, result length=%d]", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), result.size()));
            return response;
        }

        if (engine.startsWith("smart")) {
            sql = conf + "--conf olap-engine-mode=smart\n" + querySql;
        } else {
            sql = conf + querySql;
        }

        QueryResult qr = queryResultMapper.listByQueryID(queryHistory.getId());
        if (null != qr) {
            HashMap<String, Object> data = new HashMap<>();
            JSONObject columns = JSONObject.parseObject(qr.getColumnsStr(), JSONObject.class);
            List<String> column = (List<String>) columns.get("column");
            List<Map<String, Object>> result = JSONObject.parseObject(qr.getResultStr(),
                    List.class);
            data.put("status", 0);
            data.put("uuid", uuid);
            data.put("meta", column);
            data.put("repeat_meta", columns.get("repeat"));
            data.put("result", result);
            data.put("engine", queryHistory.getEngine());
            data.put("queryId", queryHistory.getQueryId());
            data.put("id", queryHistory.getId());
            data.put("querySql", querySql);
            data.put("querySqlParam", queryHistory.getQuerySqlParam());
            data.put("param", JSONObject.parseObject(queryHistory.getParam()));
            data.put("fileSize", queryHistory.getDataSize());
            data.put("scanSize", queryHistory.getEngine().startsWith("presto") ? FileUtil.getStringSize(queryHistory.getProcessedBytes()) : "-");
            data.put("executeDuration", queryHistory.getExecuteDuration() == null ? 0 : queryHistory.getExecuteDuration());
            data.put("region", queryHistory.getRegion());
            data.put("catalog", queryHistory.getCatalog());
            data.put("type", JSONArray.parseArray(queryHistory.getColumnType()));
            response.put("data", data);
            response.put("code", 0);
            log.info(String.format("%s read the historical query data from mysql[id=%d, uuid=%s] successfully[queryId=%s, result length=%d]",
                    user, queryHistory.getId(), uuid, queryHistory.getQueryId(), result.size()));
            return response;
        }

        String region = queryHistory.getRegion();
        Properties properties = new Properties();
        properties.setProperty("user", username);
        if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
            url = huaweiUrl;
        } else {
            if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
                url = awsSGUrl;
            } else if (!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
                url = olapUrl.getUrl().get(queryHistory.getRegion());
            } else {
                url = awsUrl;
            }
            boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(queryHistory.getRegion()));
            if (use_ssl) {
                properties.setProperty("password", password);
            }
            properties.setProperty("SSL", olapUrl.getSslProperty().get(queryHistory.getRegion()));
        }

        ResultSet rs = null;
        Statement statement = null;
        Connection connection = null;
        try {
            log.info(String.format("[user=%s] start to connect url[%s] when historical query data[id=%d, uuid=%s]", user, url, queryHistory.getId(), uuid));
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);

            log.info(String.format("%s start to retrieve data when historical query data[id=%d, uuid=%s]", user, queryHistory.getId(), uuid));
            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<String> column = (ArrayList<String>) columns.get("column");
            ArrayList<Map<String, String>> types = (ArrayList<Map<String, String>>) columns.get("type");
            ArrayList<Map<String, Object>> result = getResult(rs, column, types, true);
            rs.close();
            statement.close();
            connection.close();

            HashMap<String, Object> data = new HashMap<>();
            if (result.size() == 0 && ((column.size() == 1 && column.get(0).equals("Result")) || column.size() == 0) && sql.toLowerCase().contains("create table")) {
                column = new ArrayList<String>();
                data.put("message", "创建成功");
            }

            data.put("status", 0);
            data.put("uuid", uuid);
            data.put("meta", column);
            data.put("repeat_meta", columns.get("repeat"));
            data.put("result", result);
            data.put("engine", queryHistory.getEngine());
            data.put("queryId", queryHistory.getQueryId());
            data.put("id", queryHistory.getId());
            data.put("querySql", querySql);
            data.put("querySqlParam", queryHistory.getQuerySqlParam());
            data.put("param", JSONObject.parseObject(queryHistory.getParam()));
            data.put("fileSize", queryHistory.getDataSize());
            data.put("scanSize", queryHistory.getEngine().startsWith("presto") ? FileUtil.getStringSize(queryHistory.getProcessedBytes()) : "-");
            data.put("executeDuration", queryHistory.getExecuteDuration() == null ? 0 : queryHistory.getExecuteDuration());
            data.put("region", queryHistory.getRegion());
            data.put("catalog", queryHistory.getCatalog());
            data.put("type", JSONArray.parseArray(queryHistory.getColumnType()));
            response.put("data", data);
            response.put("code", 0);
            log.info(String.format("%s execute the historical query data[id=%d, uuid=%s] successfully[queryId=%s, result length=%d]", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), result.size()));
            saveQueryResult(queryHistory.getId(), result, columns, InfTraceContextHolder.get().getTenantName());
        } catch (Exception e) {
            log.error(String.format("%s failed to query historical data[id=%d, uuid=%s, queryId=%s]: %s", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), CommonUtil.printStackTraceToString(e)));
            try {
                throw e;
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                String message = ex.getMessage().trim();
                log.error(String.format("There is an SQLException when %s query historical data[id=%d, uuid=%s, queryId=%s, code=%d]: %s", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), code, message));
                code = code == 0 ? 34 : code;
                if (message.equals("Error executing query")) {
                    message = retrieveSQLExceptionCause(ex);
                    log.error(String.format("%s failed to retrieve data when query historical data[id=%d, uuid=%s, queryId=%s, code=%d]: %s", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), code, message));
                }
                response.put("code", code);
                response.put("message", message);
                return response;
            }
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                String message = ex.getMessage().trim();
                response.put("code", code);
                response.put("message", message);
                log.error(String.format("There is an Exception when closing the historical query data[user=%s, id=%d, uuid=%s, queryId=%s]: %s", user, queryHistory.getId(), uuid, queryHistory.getQueryId(), message));
            }
        }

        return response;
    }

    @Override
    public HashMap<String, Object> queryNewLog(String uuid, String user, Long pointer) throws InterruptedException, IOException {
        log.info(String.format("%s start to get hive log %s", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        List<String> rows = new ArrayList<>();

        String filePath = gatewayLogDir + File.separator + uuid + ".csv";
        log.info(String.format("file path[%s] when %s get hive log %s", filePath, user, uuid));
        File file = new File(filePath);
        try {
            Integer status = 0;
            QueryHistory queryHistory = null;
            if (!file.exists()) {
                Integer queryStatus = 3;
                queryHistory = queryHistoryMapper.selectByUuid(uuid);
                if (queryHistory != null && (queryHistory.getSparkLogStatus().equals(1) || queryHistory.getStatus() != 3)) {
                    status = 1;
                    queryStatus = queryHistory.getStatus();
                }
                log.info(String.format("file not exist when %s get hive log[uuid=%s, queryStatus=%d]", user, uuid, queryStatus));

                data.put("status", status);
                data.put("rows", rows);
                data.put("pointer", pointer);
                response.put("data", data);
                response.put("code", 0);
                return response;
            }

            @SuppressWarnings("resource")
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(pointer);
            String line = null;
            while ((line = raf.readLine()) != null) {
                line = new String(line.getBytes("ISO-8859-1"),"utf-8");
                rows.add(line);
            }

            queryHistory = queryHistoryMapper.selectByUuid(uuid);
            if (queryHistory != null && queryHistory.getSparkLogStatus().equals(1)) {
                status = 1;
            }

            data.put("status", status);
            data.put("rows", rows);
            data.put("pointer", raf.getFilePointer());
            response.put("data", data);
            response.put("code", 0);

            if (status == 1 && file.exists()) {
                file.delete();
            }

            log.info(String.format("%s get hive log[uuid=%s, status=%d]", user, uuid, status));
        } catch (Exception e) {
            log.error(String.format("%s failed to get hive log[uuid=%s]: %s", user, uuid, CommonUtil.printStackTraceToString(e)));
            String message = e.getMessage().trim();
            log.error(String.format("There is an Exception when %s get hive log[uuid=%s]: %s", user, uuid, message));
            response.put("code", 405);
            response.put("message", message);
            return response;
        }

        return response;
    }

    @Override
    public HashMap<String, Object> queryLog(String uuid, String user) throws InterruptedException {
        log.info(String.format("%s start to get query log %s", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = null;
        for (int i = 0; i < sparkCount; i++) {
            queryHistory = queryHistoryMapper.selectByUuid(uuid);
            if (queryHistory != null) {
                break;
            }
            Thread.sleep(sparkSleep);
        }
        if (queryHistory == null) {
            int code = 41;
            String message = String.format("There is no valid uuid[%s] when get query log", uuid);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        if (!queryHistory.getEngine().startsWith("spark")) {
            int code = 42;
            String message = String.format("[%s]It's no a valid query engine[%s] when get query log", uuid, queryHistory.getEngine());
            log.error(String.format("[%s]%s[%s]", user, message, queryHistory.getUuid()));
            response.put("code", code);
            response.put("Message", message);
            return response;
        }

        int id = queryHistory.getId();
        String querySql = String.format("select query_id from olap.query_log where request_id='%s'", uuid);

        Properties properties = new Properties();
        properties.setProperty("user", olapDbUser);
        boolean use_ssl = Boolean.valueOf(olapUrl.getSslProperty().get(queryHistory.getRegion()));
        if (use_ssl) {
            properties.setProperty("password", olapDbPwd);
        }
        properties.setProperty("SSL", olapUrl.getSslProperty().get(queryHistory.getRegion()));

        ResultSet rs = null;
        Statement statement = null;
        Connection connection = null;
        try {
            log.info(String.format("[user=%s] start to connect url[%s] when get spark query log[id=%d, uuid=%s]", user, olapDbUrl, id, uuid));
            connection = DriverManager.getConnection(olapDbUrl, properties);
            statement = connection.createStatement();

            String query_id = "";
            for (int i = 0; i < sparkCount; i++) {
                rs = statement.executeQuery(querySql);

                while (rs.next()) {
                    query_id = rs.getString(1);
                }
                rs.close();
                log.info(String.format("%s get spark query_id[%s] when get spark query log[id=%d, uuid=%s, i=%d]", user, query_id, id, uuid, i));
                if (!query_id.equals("")) {
                    break;
                }
                Thread.sleep(sparkSleep);
            }

            statement.close();
            connection.close();

            HashMap<String, Object> data = new HashMap<>();
            String url = "";
            if (query_id.contains("#")) {
                query_id = query_id.split("#")[1];
                url = genieUrl + "/jobs?id=" + query_id;
            }

            data.put("status", 0);
            data.put("url", url);
            data.put("queryId", query_id);
            data.put("uuid", uuid);
            response.put("data", data);
            response.put("code", 0);
            log.info(String.format("%s get spark query log url[id=%d, uuid=%s, queryId=%s] successfully", user, id, uuid, query_id));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(String.format("%s failed to get spark query log url[id=%d, uuid=%s]: %s", user, id, uuid, e.getMessage().trim()));
            try {
                throw e;
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                String message = ex.getMessage().trim();
                log.error(String.format("There is an Exception when %s get spark query log url[id=%d, uuid=%s]: %s", user, id, uuid, message));
                response.put("code", code);
                response.put("message", message);
                return response;
            }
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException ex) {
                int code = ex.getErrorCode();
                String message = ex.getMessage().trim();
                response.put("code", code);
                response.put("message", message);
                log.error(String.format("There is an Exception when closing the getting spark log url[user=%s, id=%d, uuid=%s]: %s", user, id, uuid, message));
            }
        }

        return response;
    }

    @Override
    public HashMap<String, Object> shareId(Integer id, String user) {
        log.info(String.format("%s start to get share info[id=%d]", user, id));
        HashMap<String, Object> response = new HashMap<>();
        SavedQuery savedHistory = savedQueryMapper.selectById(id);
        if (savedHistory == null) {
            int code = 51;
            String message = String.format("There is no valid ids[%d] when share", id);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        String engine = savedHistory.getEngine();
        String querySql = savedHistory.getQuerySql();
        String region = savedHistory.getRegion();
        String catalog = savedHistory.getCatalog();
        String param = savedHistory.getParam();
        HashMap<String, Object> data = new HashMap<>();
        data.put("engine", engine);
        data.put("querySql", querySql);
        data.put("region", region);
        data.put("catalog", catalog);
        data.put("param", JSONObject.parseObject(param));
        response.put("code", 0);
        response.put("data", data);
        log.info(String.format("%s get share info[id=%d] successfully", user, id));
        return response;
    }

    @Override
    @Async
    public void probeAsyn(String uuid, String user, JSONArray type,
                          boolean updateColumnType, JSONObject sample,
                          String tenantName) {
        InfTraceContextHolder.get().setTenantName(tenantName);
        log.info(String.format("%s start to probe csv asynchronously[%s_probe.csv]", user, uuid));

        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);

        String root = System.getProperty("user.dir");
        String fileDir = root + File.separator + "data" + File.separator + "csv";
        String filePath = fileDir + File.separator + uuid + "_probe.csv";
        File file = null;
        try {
            String engine = queryHistory.getEngine();
            Integer isAsync = getMysqlAsync(engine);
            if (engine.startsWith("mysql") && isAsync == 0) {
                queryHistory = queryHistoryMapper.selectByUuid(uuid);
                Integer mysqlAsync = queryHistory.getMysqlAsync();
                String start = TimeUtil.getNow();
                String end = TimeUtil.getNow();
                while (mysqlAsync == null && TimeUtil.getTimeDiff(start, end) < 600) {
                    log.info(String.format("Mysql query [uuid=%s] is not ready, probe is waiting ......", uuid));
                    Thread.sleep(2000);
                    end = TimeUtil.getNow();
                    queryHistory = queryHistoryMapper.selectByUuid(uuid);
                    mysqlAsync = queryHistory.getMysqlAsync();
                }
            }

            file = new File(filePath);

            log.info(String.format("%s start to update queryHistory in probe", user, uuid));
            queryHistory.setProbeStatus(1);
            update(queryHistory);
            log.info(String.format("%s update queryHistory sucessfully in probe", user, uuid));

            String downloadStart = TimeUtil.getNow();
            downloadToCsv(uuid, user, file, updateColumnType, queryHistory);
            String downloadEnd = TimeUtil.getNow();
            log.info(String.format("%s download csv asynchronously[%s.csv] successfully[downloadTime=%s]", user, uuid, TimeUtil.getTimeDiff(downloadStart, downloadEnd)));
            if (file.length() > 200000000) {
                queryHistory.setProbeStatus(-2);
                queryHistory.setProbeResult("result is too large to probe");
                update(queryHistory);
                log.info(String.format("%s csv[%s.csv] size %d is too large to probe]", user, uuid, file.length()));
                return;
            }

//            queryHistory.setProbeStatus(2);
//            update(queryHistory);
            if (type.size() == 0 && queryHistory.getStatus() == 0) {
                queryHistory = queryHistoryMapper.selectByUuid(uuid);
                String columnType = queryHistory.getColumnType();
                type = JSONObject.parseArray(columnType);
            }

            log.info(String.format("%s start to probe [%s.csv] asynchronously", user, uuid));
            String probeStart = TimeUtil.getNow();
            Map<String, Object> probeData = ProbeUtil.probeCSV(filePath, type, sample);
            String probeEnd = TimeUtil.getNow();

            log.info(String.format("%s start to update queryHistory again in probe", user, uuid));
            queryHistory.setProbeStatus(2);
            queryHistory.setTotal((Integer) probeData.get("total"));
            queryHistory.setProbeResult(JSONObject.toJSONString(probeData.get("result")));
            update(queryHistory);

            log.info(String.format("%s successfully completed asynchronous probe %s.csv[probeTime=%s]", user, uuid, TimeUtil.getTimeDiff(probeStart, probeEnd)));
        } catch (Exception e) {
            log.error(String.format("There is a stack err when %s probe asynchronously[uuid=%s]: %s", user, uuid, CommonUtil.printStackTraceToString(e)));
            try {
                throw e;
            } catch (Exception ex) {
                int code = -2;
                String message = ex.getMessage().trim();
                queryHistory.setProbeStatus(code);
                queryHistory.setProbeResult(message);
                update(queryHistory);
                log.error(String.format("There is an Exception when %s download file %s.csv asyn: %s", user, uuid, message));
            }
        } finally {
            if (file.exists()) {
                log.info(String.format("user [%s] delete probe file [%s]", user, filePath));
                file.delete();
            }
        }
    }

    @Override
    public HashMap<String, Object> getStatus(JSONArray uuidList) {
        HashMap<String, Object> statusObj = new HashMap<>();

        for (int i = 0; i < uuidList.size(); i++) {
            Integer status = 0;
            String uuid = uuidList.getString(i);
            QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
            if (queryHistory != null) {
                status = queryHistoryMapper.selectByUuid(uuid).getStatus();
            }

            statusObj.put(uuid, status);
        }

        return statusObj;
    }

    @Override
    public int getShare(String sharee, Integer gradeID) {
        ShareGrade sg = sgMapper.selectByPrimaryKey(gradeID);
        if (null == sg) {
            return -1;
        }

        if (!sharee.equalsIgnoreCase(sg.getSharee())) {
            return -2;
        }

        return sg.getGrade();
    }

    private HashMap<String, String> getErrorInfo(String errOrigin) {
        HashMap<String, String> infoMap = new HashMap<>();
        log.info(String.format("getErrorInfo decoded error info is %s", errOrigin));
        infoMap.put("errorType", "");
        infoMap.put("errorZh", "");
        String errorType = "";
        String errorZh = "";
        for (int i = 0; i < listRegex.size(); i++) {
            Matcher m = listRegex.get(i).matcher(errOrigin);
            if (m.find()) {
                errorType = new String(listEr.get(i).getErrorType());
                errorZh = new String(listEr.get(i).getErrorZh());
                for (int j = 1; ; ) {
                    String num = "{" + String.valueOf(j) + "}";
                    if (errorZh.contains(num)) {
                        errorZh = errorZh.replace(num, m.group(j));
                        ++j;
                    } else {
                        break;
                    }
                }
                infoMap.put("errorType", errorType);
                infoMap.put("errorZh", errorZh);
                break;
            }
        }
        return infoMap;
    }

    @Override
    public HashMap<String, String> testErrInfo(String err) {
        try {
            String errOrigin = URLDecoder.decode(err, StandardCharsets.UTF_8.name());
            return getErrorInfo(errOrigin);
        } catch (Exception e) {
            log.info(String.format("testErrorInfo exception %s", e.getMessage()));
            return null;
        }
    }

    @Override
    public HashMap<String, Object> statsInfo(Integer step, String uuid, String query_id) {
        HashMap<String, Object> statusObj = new HashMap<>();
        QueryHistory queryHistory = super.getByUuid(uuid);
        if (null == queryHistory || queryHistory.getId() == 0) {
            statusObj.put("code", 11);
            statusObj.put("message", "uuid is not exist");
            return statusObj;
        }

        String region = queryHistory.getRegion();
        String engine = queryHistory.getEngine();
        String url;
        if (engine.equals("presto_huawei") || engine.equals("smart_huawei")) {
            url = huaweiUrl;
        } else {
            if (engine.equals("presto_aws_sg") || engine.equals("smart_aws_sg")) {
                url = awsSGUrl;
            } else if (!region.equals("aws_ue1") && !region.equals("aws_sg") && !region.equals("huawei_sg")) {
                url = olapUrl.getUrl().get(queryHistory.getRegion());
            } else {
                url = awsUrl;
            }
        }
        String domain = getDomain(url, engine);

        try {
            if (1 == step) {
                String resInfo = CommonUtil.httpResult(domain + "v1/queryID/" + uuid, true, null, null, uuid);
                statusObj.put("query_id", resInfo);
                return statusObj;
            }

            if (3 == step || 2 == step) {
                String resInfo = CommonUtil.httpResult(domain + "ui/api/query/state/" + query_id, true, null, null, uuid);
                Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
                String tipInfo = "";
                double progress = 1.0;
                for (Map.Entry<String, Object> entry : content.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key.equalsIgnoreCase("activeWorkers")) {
                        if (Long.valueOf(value.toString()) < 10) {
                            tipInfo += "集群资源较少\n";
                        }
                    } else if (key.equalsIgnoreCase("queuedQueries")) {
                        if (Long.valueOf(value.toString()) > 0) {
                            tipInfo += "集群资源紧张\n";
                        }
                    } else if (key.equalsIgnoreCase("physicalInputDataSize")) {
                        Long dSize = Long.valueOf(value.toString());
                        statusObj.put(key, FileUtil.getStringSize(dSize));
                        if (dSize > 5000000000000L) {
                            tipInfo += "扫描数据量大\n";
                        }
                    } else if (key.equalsIgnoreCase("outputRows")) {
                        if (Long.valueOf(value.toString()) > 3000000) {
                            tipInfo += "结果数据量大\n";
                        }
                    } else if (key.equalsIgnoreCase("scanSplits")) {
                        if (Long.valueOf(value.toString()) > 500000) {
                            tipInfo += "扫描文件数过多\n";
                        }
                    } else if (key.equalsIgnoreCase("skewIndex")) {
                        if (Double.valueOf(value.toString()) > 2) {
                            tipInfo += "数据倾斜严重\n";
                        }
                    } else if (key.equalsIgnoreCase("queryState")) {
                        statusObj.put(key, value);
                    } else if (key.equalsIgnoreCase("totalDrivers")) {
                        Long v_num = Long.valueOf(value.toString());
                        if (0 != v_num) {
                            progress /= v_num;
                        }
                    } else if (key.equalsIgnoreCase("completedDrivers")) {
                        progress *= Long.valueOf(value.toString());
                    }
                }
                statusObj.put("progress", progress);
                if (null != tipInfo && !tipInfo.isEmpty()) {
                    statusObj.put("tipInfo", tipInfo);
                }
            }
            if (2 == step) {
                String resInfo = CommonUtil.httpResult(timeestUrl, false, queryHistory.getQuerySql(), null, uuid);
                Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
                statusObj.put("estimatedSecond", Double.valueOf(content.get("time").toString()));
            }
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while parse stats info: %s",
                    uuid, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(e.getMessage(), e.getMessage());
        }

        return statusObj;
    }

    @Override
    public Map<String, Object> getShares(int pageNum, int pageSize,
                                         String share_sql, String sharer, String engine,
                                         String region, String sharee) {
        List<HashMap<String, Object>> statusList = new ArrayList<HashMap<String, Object>>();
        List<ShareGrade> sgList = sgMapper.listBySharee(sharee, sharer);
        if (null != sgList) {
            for (int i = 0; i < sgList.size(); ++i) {
                HashMap<String, Object> statusObj = new HashMap<>();
                ShareGrade sg = sgList.get(i);
                String shareUrl = sg.getShareUrl();
                if (shareUrl.indexOf("query?uuid=") != -1) {
                    String uuid = shareUrl.split("=")[1];
                    QueryHistory queryHistory = super.getByUuid(uuid);
                    if (null != queryHistory && queryHistory.getId() != 0) {
                        if (share_sql.length() > 0 &&
                                -1 == queryHistory.getQuerySql().toLowerCase().indexOf(share_sql)) {
                            continue;
                        }
                        if (engine.length() > 0 &&
                                -1 == queryHistory.getEngine().toLowerCase().indexOf(engine)) {
                            continue;
                        }
                        if (region.length() > 0 &&
                                -1 == queryHistory.getRegion().toLowerCase().indexOf(region)) {
                            continue;
                        }
                        statusObj.put("sql", queryHistory.getQuerySql());
                        statusObj.put("engine", queryHistory.getEngine());
                        statusObj.put("region", queryHistory.getRegion());
                        statusObj.put("catalog", queryHistory.getCatalog());
                    } else {
                        continue;
                    }
                } else if (shareUrl.indexOf("query?sqlId=") != -1) {
                    String id = shareUrl.split("=")[1];
                    SavedQuery savedQuery = savedQueryMapper.selectById(Integer.valueOf(id));
                    if (null != savedQuery && savedQuery.getId() != 0) {
                        if (share_sql.length() > 0 &&
                                -1 == savedQuery.getQuerySql().toLowerCase().indexOf(share_sql)) {
                            continue;
                        }
                        if (engine.length() > 0 &&
                                -1 == savedQuery.getEngine().toLowerCase().indexOf(engine)) {
                            continue;
                        }
                        if (region.length() > 0 &&
                                -1 == savedQuery.getRegion().toLowerCase().indexOf(region)) {
                            continue;
                        }
                        statusObj.put("sql", savedQuery.getQuerySql());
                        statusObj.put("engine", savedQuery.getEngine());
                        statusObj.put("region", savedQuery.getRegion());
                        statusObj.put("catalog", savedQuery.getCatalog());
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                statusObj.put("id", sg.getId());
                statusObj.put("sharer", sg.getSharer());
                statusObj.put("grade", sg.getGrade());
                statusObj.put("shareUrl", shareUrl);
                statusObj.put("name", sg.getSqlName());
                statusList.add(statusObj);
            }
        }
        int startIdx = (pageNum - 1) * pageSize;
        int endIdx = pageNum * pageSize;
        List<HashMap<String, Object>> resList = new ArrayList<HashMap<String, Object>>();
        if (startIdx >= 0 && startIdx < statusList.size()) {
            if (endIdx > statusList.size()) {
                endIdx = statusList.size();
            }
            resList = statusList.subList(startIdx, endIdx);
        }
        Map<String, Object> mapRes = new HashMap<String, Object>();
        mapRes.put("list", resList);
        mapRes.put("total", statusList.size());
        return mapRes;
    }

    @Async
    private void saveQueryResult(int query_id, ArrayList<Map<String, Object>> result,
                                 JSONObject columns, String tenantName) {
        try {
            InfTraceContextHolder.get().setTenantName(tenantName);
            String result_str = JSONObject.toJSONString(result);
            String columns_str = JSONObject.toJSONString(columns);
            QueryResult qr = new QueryResult();
            qr.setQueryIncId(query_id);
            qr.setResultStr(result_str);
            qr.setColumnsStr(columns_str);
            qr.setCreateTime(Timestamp.valueOf(TimeUtil.getNow()));
            queryResultMapper.insertUseGeneratedKeys(qr);
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while save query result %d: %s",
                    query_id, CommonUtil.printStackTraceToString(e)));
        }
    }

    @Override
    public HashMap<String, Object> checkSmartEngine(String uuid, String querySql,
                                                    String user, String region) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (region.equals("sg3")) {
                return response;
            }
            String resInfo = CommonUtil.httpResult(smartEngineUrl, false, querySql, null, uuid);
            Map<String, Object> content = JSON.parseObject(resInfo, Map.class);
            if (0 == Integer.valueOf(content.get("code").toString())) {
                return response;
            }

            String smart_engine = "spark-submit-sql-3_aws_us-east-1";
            if (region.equalsIgnoreCase("aws_sg")) {
                smart_engine = "spark-submit-sql-3_aws_ap-southeast-1";
            } else if (region.equalsIgnoreCase("huawei_sg")) {
                smart_engine = "spark-submit-sql-3_huawei_ap-southeast-3";
            }
            String message = String.format("you have smart engine when executing query[uuid=%s]: %s",
                    uuid, smart_engine);
            log.error(String.format("%s has smart engine when executing query[uuid=%s]: %s",
                    user, uuid, smart_engine));
            response.put("code", 1001);
            response.put("message", message);
            response.put("data", 1001);
        } catch (Exception e) {
            log.error(String.format("%s There is an exception occurred while parse smart engine info: %s",
                    uuid, CommonUtil.printStackTraceToString(e)));
        }
        return response;
    }

    @Override
    public Map<String, String> transSQLtoSpark(String querySql, String user) {
        SqlTransformer transformer = new SqlTransformer();
        SqlResult res = transformer.transformTrino2Spark(querySql);
        Map<String, String> resMap = new HashMap<>();
        resMap.put("res", res.getRes());
        resMap.put("message", res.getMessage());
        resMap.put("sparkSql", res.getSparkSql());
        resMap.put("trinoSql", res.getTrinoSql());
        if (!res.getRes().equals("SUCCESS")) {
            log.error(String.format("%s transfer sql failed, msg: %s",
                    user, res.getMessage()));
        }
        tsMapper.addTransSql(user, querySql, res.getSparkSql());
        return resMap;
    }
}
