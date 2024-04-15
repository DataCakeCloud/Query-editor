package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.*;
import com.ushareit.query.configuration.GatewayConfig;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.constant.ProbeConstant;
import com.ushareit.query.mapper.*;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.GatewayService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.web.utils.*;
import com.ushareit.query.web.utils.GatewayUtil.GWProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveQueryResultSet;
import org.apache.hive.jdbc.HiveStatement;
import com.ushareit.query.exception.ServiceException;
import org.apache.hive.service.rpc.thrift.TOperationHandle;
import org.apache.hive.service.rpc.thrift.TSessionHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ushareit.query.constant.BaseResponseCodeEnum.FAILED_TO_CLOSE_CONNECTION;

@Slf4j
@Service
public class GatewayServiceImpl extends AbstractBaseServiceImpl<QueryHistory> implements GatewayService {
    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${gateway.logDir}")
    private String gatewayLogDir;

    @Value("${de.gateway}")
    private String deUrl;
    @Value("${cluster-manager.url}")
    private String urlClusterManager;
    @Value("${cancel.sleep}")
    private int cancelSleep;
    @Value("${cancel.count}")
    private int cancelCount;
    @Value("${count.display}")
    private int displayCount;

    @Value("${count.download}")
    private int downloadCount;

    @Autowired
    private GatewayConfig gatewayConfig;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private QueryResultMapper queryResultMapper;

    @Resource
    private ErrorInfoMapper erMapper;

    @Resource
    private AccountMapper accountMapper;

    private List<ErrorInfo> listEr;
    private List<Pattern> listRegex;

    @PostConstruct
    public void initRegex() {
        listEr = erMapper.selectAll();
        listRegex = new ArrayList<Pattern>();
        for (int i = 0; i < listEr.size(); i++) {
            log.info(String.format("add regex %s", listEr.get(i).getErorRegex()));
            Pattern r = Pattern.compile(listEr.get(i).getErorRegex());
            listRegex.add(r);
        }
    }

    @Override
    public CrudMapper<QueryHistory> getBaseMapper() {
        return queryHistoryMapper;
    }

    @Override
    public HashMap<String, Object> execute(String uuid, String engineOriginal, String querySql,
                                           String region, String catalog, String database,
                                           CurrentUser currentUser, String token,
                                           String userInfo, Integer taskId, String executionDate) throws ParseException, SQLException {
        String start = TimeUtil.getNow();
        int errData = 0;
        String engine = engineOriginal;
        String user = currentUser.getUserName();
        String groupId = currentUser.getGroupIds();
        int tenantId = currentUser.getTenantId();
        String tenantName = currentUser.getTenantName();

        log.info(String.format("%s start to execute query[uuid=%s, engine=%s]", user, uuid, engine));
        QueryHistory queryHistory = new QueryHistory();
        HashMap<String, Object> response = new HashMap<>();

        queryHistory.setCreateBy(user);
        queryHistory.setUpdateBy(user);
        queryHistory.setEngine(engineOriginal);
        queryHistory.setQuerySql(querySql);
        queryHistory.setStartTime(Timestamp.valueOf(start));
        queryHistory.setIsDatabend(1);
        queryHistory.setRegion(region);
        queryHistory.setCatalog(catalog);
        queryHistory.setGroupId(groupId);
        queryHistory.setTenantId(tenantId);
        queryHistory.setFromOlap(false);
        queryHistory.setTaskId(taskId);
        queryHistory.setUserGroup(currentUser.getGroupUuid());

        queryHistory.setUuid(uuid);
        if (checkUuid(queryHistory) == 11) {
            String message = String.format("uuid is not unique[%s]", uuid);
            response.put("code", 11);
            response.put("message", message);
            response.put("data", errData);
            return response;
        }

        log.info(String.format("%s start to get config when executing query[uuid=%s]", user, uuid));
        String engine_label = engineOriginal;
        if (engine_label.startsWith("presto")) {
            engine_label = "Ares";
        }
        Meta engineInfo = metaMapper.listByKey(engineOriginal);
        if (null != engineInfo) {
            engine_label = engineInfo.getEngineName();  // 获取引擎标签
        }
        log.info(String.format("%s engine is %s when executing query[uuid=%s]", user, engine_label, uuid));

        queryHistory.setStatus(3);
        queryHistory.setStatusZh("运行中");
        queryHistory.setEngineLabel(engine_label);

        save(queryHistory);
        log.info(String.format("[user=%s]store original info to DB when executing query[uuid=%s]", user, uuid));

        String queryId = "";
        HiveStatement statement = null;
        HiveConnection connection = null;
        Properties properties = new Properties();
        GWProperties gwProperties = new GWProperties(currentUser, gatewayUrl, querySql);

        String outputPath = null;
//        Boolean isSelect = SqlUtil.isSelect(querySql);
//        log.info(String.format("[user=%s, uuid=%s] start to get output %s", user, uuid, isSelect));
//        if (isSelect) {
        try {
            outputPath = getStoragePath(tenantId, region, userInfo);
            log.info(String.format("[user=%s, uuid=%s] get output: %s", user, uuid, outputPath));
        } catch (Exception e) {
            log.info(String.format("[user=%s] failed to get output: %s", user, e.getMessage()));
        }
        GatewayUtil.setProperties(gwProperties, properties, gatewayConfig,
                engine, region, catalog, database, deUrl, token, outputPath,
                String.valueOf(queryHistory.getId()), accountMapper);

        HiveQueryResultSet rs = null;
        long peakMemoryBytes = 0;
        long cpuTimeMillis = 0;
        long wallTimeMillis = 0;
        long elapsedTimeMillis = 0;
        long queuedTimeMillis = 0;
        long processedRows = 0;
        long processedBytes = 0;

        int code = -1;
        FileWriter writer = null;
        boolean b = engine.startsWith("presto") || engine.startsWith("ares") || engine.startsWith("smart");
        try {
            String url = gwProperties.getUrlWithParams();
            url += String.format(";catalog.probe.taskId=%d;catalog.probe.from=QE", queryHistory.getId());
            List<String> sqlList = gwProperties.getSqlWithoutConf();

            log.info(String.format("[user=%s]start to connect url[%s] when executing query[id=%d, uuid=%s]",
                    user, url, queryHistory.getId(), uuid));
            connection = (HiveConnection) DriverManager.getConnection(url, properties);
            Field f = connection.getClass().getDeclaredField("sessHandle");
            f.setAccessible(true);
            TSessionHandle sessionHandle = (TSessionHandle) f.get(connection);
            String sessionID = GatewayUtil.convertBytesToUUID(sessionHandle.getSessionId().getGuid()).toString();

            File file = null;
            if (engine.startsWith("hive")) {
                String filePath = gatewayLogDir + File.separator + uuid + ".csv";
                log.info(String.format("%s start to generate hive log[%s] when executing query[id=%d, uuid=%s, queryId=%s]",
                        user, filePath, queryHistory.getId(), uuid, queryId));
                file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                writer = new FileWriter(file);;
            }
            for (int i=0; i<sqlList.size(); i++) {
                String renderSql = CommonUtil.renderSql(deUrl, token, sqlList.get(i), uuid, executionDate);
                JSONObject parse = JSONObject.parseObject(renderSql);
                if (parse.getInteger("code") != 0) {
                    throw new ServiceException(renderSql, renderSql);
                }
                String sql = parse.getString("data");
                statement = (HiveStatement) connection.createStatement();
                log.info(String.format("[user=%s] start to execute sql when executing query[count=%d, id=%d, uuid=%s]",
                        user, i, queryHistory.getId(), uuid));
                if (engine.startsWith("presto")) {
//                    statement.executeAsync(sqlList.get(i));
                    statement.execute(sql);
                    queryId = statement.getQueryId();
                    queryHistory.setQueryId(queryId);
                    update(queryHistory);
                    log.info(String.format("%s start to get query id when executing query[id=%d, uuid=%s, queryId=%s]",
                            user, queryHistory.getId(), uuid, queryId));
                } else {
                    if (engine.startsWith("hive")) {
                        try {
                            HiveStatement finalStatement = statement;
                            final FileWriter finalWriter = writer;
                            Thread logThread = new Thread(() -> {
                                try {
                                    log.info(String.format("%s generate hive log when Thread1[id=%d, uuid=%s]",
                                            user, queryHistory.getId(), uuid));
                                    while (finalStatement.hasMoreLogs()) {
                                        log.info(String.format("%s generate hive log2 when Thread2[id=%d, uuid=%s]",
                                                user, queryHistory.getId(), uuid));
                                        List<String> queryLog = finalStatement.getQueryLog();
                                        log.info(String.format("%s generate hive log when executing query1[id=%d, uuid=%s]: %s",
                                                user, queryHistory.getId(), uuid, Arrays.toString(queryLog.toArray())));
                                        for (String str : queryLog) {
                                            if (!str.isEmpty()) {
                                                finalWriter.write(str + "\n");
                                            }
                                        }
                                        finalWriter.flush();
                                        if (queryLog.isEmpty()) {
                                            Thread.sleep(1000L);
                                        }
                                    }
                                } catch (SQLException | IOException | InterruptedException e) {
                                    log.info(String.format("%s failed to generate hive log when executing query[id=%d, uuid=%s]: %s",
                                            user, queryHistory.getId(), uuid, CommonUtil.printStackTraceToString(e)));
                                }
                            });
                            logThread.start();
//                            logThread.join(); // 等待日志写入线程完成
                        } catch (Exception e) {
                            log.error(String.format("%s failed to write hive log[id=%s]: %s", currentUser.getUserName(), queryHistory.getId(), CommonUtil.printStackTraceToString(e)));
                        }
                    }
                    statement.execute(sql);
                }
                if (i != sqlList.size()-1) {
                    statement.close();
                }
            }
            String operationId = "";
            Field stmtHandle = statement.getClass().getDeclaredField("stmtHandle");
            stmtHandle.setAccessible(true);
            TOperationHandle op = (TOperationHandle) stmtHandle.get(statement);
            if (op != null) {
                operationId = GatewayUtil.convertBytesToUUID(op.getOperationId().getGuid()).toString();
            }

            log.info(String.format("%s start to retrieve data when executing query[id=%d, uuid=%s, ssid=%s, op=%s]",
                    user, queryHistory.getId(), uuid, sessionID, op));

            rs = (HiveQueryResultSet) statement.getResultSet();

            queryHistory.setSessionId(sessionID);
            if (!engine.startsWith("presto")) {
                queryId = sessionID;
                queryHistory.setQueryId(queryId);
                queryHistory.setSparkLogStatus(1);
                update(queryHistory);
            }

            HashMap<String, Object> data = new HashMap<>();
            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<String> column = (ArrayList<String>) columns.get("column");
            ArrayList<Map<String, String>> types = (ArrayList<Map<String, String>>) columns.get("type");

            Integer resultLen = 0;
            ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            log.info(String.format("%s start to get result when executing query[id=%d, uuid=%s]",
                    user, queryHistory.getId(), uuid));
            result = getResult(rs, column, types, false);
            resultLen = result.size();
            log.info(String.format("%s get result success when executing query[id=%d, uuid=%s]", user, queryHistory.getId(), uuid));

            String size = fileSize(tenantId, sessionID, user, region, userInfo);
            log.info(String.format("%s query file size successfully[result length=%d] when executing query[id=%d, uuid=%s]",
                    user, resultLen, queryHistory.getId(), uuid));

            //elapsedTimeMillis = rs.getStats().getElapsedTimeMillis();
            String end = TimeUtil.getNow();
            float executeDuration = elapsedTimeMillis > 0 ? TimeUtil.getTimeFloat(elapsedTimeMillis) / 1000 : TimeUtil.getTimeDiff(start, end);
            BigDecimal bCute = new BigDecimal(executeDuration);
            executeDuration = bCute.setScale(2, RoundingMode.UP).floatValue();

            queryHistory.setStatus(0);
            queryHistory.setStatusZh("已完成");
            queryHistory.setOperationId(operationId);
            queryHistory.setExecuteDuration(executeDuration);
            queryHistory.setColumnType(JSONObject.toJSONString(columns.get("type")));
            //queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得
            queryHistory.setQueryId(queryId);
            update(queryHistory);
            log.info(String.format("%s start to get a new query id when executing query[id=%d, uuid=%s, queryId=%s]",
                    user, queryHistory.getId(), uuid, queryId));

            //processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
            log.info(String.format("%s picked processedBytes when executing query[id=%d, uuid=%s]",
                    user, queryHistory.getId(), uuid));
            String scanSize = b ? FileUtil.getStringSize(processedBytes) : "-";
            if (result.size() == 0 && ((column.size() == 1 && column.get(0).equals("Result")) || column.size() == 0)) {
                column = new ArrayList<String>();
                data.put("message", "执行成功");
                size = "";
            }
            if (result.size() > 0) {
                JSONObject sample = JSONObject.parseArray(JSONObject.toJSONString(result)).getJSONObject(0);
                queryHistory.setProbeSample(sample.toJSONString());
            }

            queryHistory.setDataSize(size);
            queryHistory.setScanSize(scanSize);
            queryHistory.setProcessedBytes(processedBytes);
            data.put("meta", column);
            data.put("type", columns.get("type"));
            data.put("repeat_meta", columns.get("repeat"));
            data.put("queryId", queryId);
            data.put("id", queryHistory.getId());
            data.put("status", 0);
            data.put("result", result);
            data.put("fileSize", size);
            data.put("scanSize", scanSize);
            data.put("executeDuration", executeDuration);
            data.put("sql", querySql);
            response.put("data", data);
            response.put("code", 0);
            update(queryHistory);
            log.info(String.format("%s execute the query[id=%d, uuid=%s] successfully[queryId=%s]",
                    user, queryHistory.getId(), uuid, queryId));
            saveQueryResult(queryHistory.getId(), result, columns, tenantName);
        } catch (Exception e) {
            code = 13;
            log.error(String.format("There is a stack err when %s executing query[id=%d, uuid=%s, errCode=%d]: %s",
                    user, queryHistory.getId(), uuid, code, CommonUtil.printStackTraceToString(e)));
            try {
                if (engine.startsWith("presto")) {
                    queryId = statement.getQueryId();
                } else if (null != rs) {
                    Field[] fs = rs.getClass().getDeclaredFields();
                    String sessionID = UUID.randomUUID().toString();
                    for (Field f : fs) {
                        f.setAccessible(true);
                        if (f.getName().equals("sessHandle")) {
                            TSessionHandle sessionHandle = (TSessionHandle) f.get(rs);
                            sessionID = GatewayUtil.convertBytesToUUID(
                                    sessionHandle.getSessionId().getGuid()).toString();
                            break;
                        }
                    }
                    queryId = sessionID;
                }
            } catch (Exception ex) {
                log.error(String.format("%s failed to get query id[id=%d, uuid=%s, queryId=%s] %s",
                        user, queryHistory.getId(), uuid, queryId, ex.getMessage()));
            }

            try {
                throw e;
            } catch (Exception ex) {
                String message = "";
                if (ex.getClass() == SQLException.class) {
                    code = ((SQLException) ex).getErrorCode();
                    message = ex.getMessage();
                    log.error(String.format("There is a SQLException when %s executing query[id=%d, uuid=%s, originalCode=%d]: %s", user, queryHistory.getId(), uuid, code, message));
                    if (code == 0 && message.equals("Error executing query")) {
                        code = 15;
                        message = String.format("failed to retrieve data: %s", retrieveSQLExceptionCause((SQLException) ex));
                        log.error(String.format("%s failed to retrieve data when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, queryId, code, message));
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
                        log.error(String.format("%s cancelled task when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, message));
                        return response;
                    }
                    if (code == 403) {
                        message = String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, message);
                        errData = 403;
                        log.error(String.format("%s has no permission when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, message));
                    }
                    if (code == 65543 && message.contains("Compiler failed")) {
                        message = ((SQLException) ex).getCause().getCause().toString();
                    }
                    if (code == 0 && message.contains("SQL statement is not a query") && message.toLowerCase().contains("create table") && message.toLowerCase().contains("select")) {
                        code = 55;
                        log.error(String.format("%s is creating table using 'create table XX as'[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, message));
                    }
                    if (code == 0 && message.contains("SQL statement is not a query") && message.toLowerCase().contains("insert into") && message.toLowerCase().contains("select")) {
                        code = 56;
                        log.error(String.format("%s is insert into table using 'insert into XX'[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, message));
                    }
                    if (1001 == code) {
                        String smart_engine = "spark-submit-sql-3_aws_us-east-1";
                        if (region.equalsIgnoreCase("aws_sg")) {
                            smart_engine = "spark-submit-sql-3_aws_ap-southeast-1";
                        } else if (region.equalsIgnoreCase("huawei_sg")) {
                            smart_engine = "spark-submit-sql-3_huawei_ap-southeast-3";
                        }
                        message = String.format("you have smart engine when executing query[id=%d, uuid=%s, errCode=%d]: %s", queryHistory.getId(), uuid, code, smart_engine);
                        errData = 1001;
                        log.error(String.format("%s has smart engine when executing query[id=%d, uuid=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, code, smart_engine));
                    }
                } else if (ex.getClass() == DataIntegrityViolationException.class) {
                    code = 1406;
                    message = ex.getCause().toString().trim();
                    log.error(String.format("%s failed to save sql when executing query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, queryId, code, ((DataIntegrityViolationException) ex).getMessage()));
                    queryHistory.setQuerySql("");
                    save(queryHistory);
                } else {
                    message = ex.getMessage();
                    if (message == null || message.equals("-1") || message.equals("null")) {
                        message = CommonUtil.printStackTraceToString(ex);
                        message = stripStackInfo(message);
                    }
                }
                log.error(String.format("%s failed to query[id=%d, uuid=%s, queryId=%s, errCode=%d]: %s", user, queryHistory.getId(), uuid, queryId, code, message));
                message = String.format("[taskID=%d, queryID=%s]%s", queryHistory.getId(), queryId, message);

                float executeDuration = TimeUtil.getTimeDiff(start, TimeUtil.getNow());
                BigDecimal bCute = new BigDecimal(executeDuration);
                executeDuration = bCute.setScale(2, RoundingMode.UP).floatValue();
                code = code > 0 ? code : 500;
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
                    log.error(String.format("%s failed to query[id=%d, uuid=%s, code=%d], the result returned to the front end is: %s", user, queryHistory.getId(), uuid, code, response.toString()));
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
                    data.put("id", queryHistory.getId());
                    data.put("status", 0);
                    data.put("result", result);
                    data.put("fileSize", "");
                    data.put("scanSize", "-");
                    data.put("executeDuration", executeDuration);
                    data.put("message", "创建成功");
                    response.put("data", data);
                    response.put("code", 0);
                    log.info(String.format("%s create table[id=%d, uuid=%s] successfully[queryId=%s]", user, queryHistory.getId(), uuid, queryId));
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
                    data.put("id", queryHistory.getId());
                    data.put("status", 0);
                    data.put("result", result);
                    data.put("fileSize", "");
                    data.put("scanSize", "-");
                    data.put("executeDuration", executeDuration);
                    data.put("message", "执行成功");
                    response.put("data", data);
                    response.put("code", 0);
                    log.info(String.format("%s create table[id=%d, uuid=%s] successfully[queryId=%s]", user, queryHistory.getId(), uuid, queryId));
                }
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                log.info(String.format("%s failed to close spark log writer when executing query[id=%d, uuid=%s]: %s",
                        user, queryHistory.getId(), uuid, CommonUtil.printStackTraceToString(ex)));
            }

            String scanSize = "-";
            if ((engine.startsWith("presto") && null != statement) || rs != null) {
                /*peakMemoryBytes = rs.unwrap(PrestoResultSet.class).getStats().getPeakMemoryBytes();
                cpuTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getCpuTimeMillis();
                wallTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getWallTimeMillis();
                elapsedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getElapsedTimeMillis();
                queuedTimeMillis = rs.unwrap(PrestoResultSet.class).getStats().getQueuedTimeMillis();
                processedRows = rs.unwrap(PrestoResultSet.class).getStats().getProcessedRows();
                queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得*/
                if (null == queryId || queryId.isEmpty()) {
                    try {
                        if (engine.startsWith("presto")) {
                            queryId = ((HiveStatement) statement).getQueryId();
                        } else {
                            Field[] fs = rs.getClass().getDeclaredFields();
                            String sessionID = UUID.randomUUID().toString();
                            for (Field f : fs) {
                                f.setAccessible(true);
                                if (f.getName().equals("sessHandle")) {
                                    TSessionHandle sessionHandle = (TSessionHandle) f.get(rs);
                                    sessionID = GatewayUtil.convertBytesToUUID(
                                            sessionHandle.getSessionId().getGuid()).toString();
                                    break;
                                }
                            }
                            queryId = sessionID;
                        }
                    } catch (Exception ex) {
                        log.error(String.format("%s failed to get query id[id=%d, uuid=%s, queryId=%s] %s",
                                user, queryHistory.getId(), uuid, queryId, ex.getMessage()));
                    }
                }
                if (code != -1) {
                    //processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
                    queryHistory.setProcessedBytes(processedBytes);
                    scanSize = b ? FileUtil.getStringSize(processedBytes) : "-";
                    queryHistory.setScanSize(scanSize);
                }
            }

            queryHistory.setSparkLogStatus(1);
            queryHistory.setPeakMemoryBytes(peakMemoryBytes);
            queryHistory.setCpuTimeMillis(cpuTimeMillis);
            queryHistory.setWallTimeMillis(wallTimeMillis);
            queryHistory.setElapsedTimeMillis(elapsedTimeMillis);
            queryHistory.setQueuedTimeMillis(queuedTimeMillis);
            queryHistory.setProcessedRows(processedRows);
            queryHistory.setQueryId(queryId);
            update(queryHistory);
            log.info(String.format("%s start to get a new query id when query is finished[id=%d, uuid=%s, queryId=%s, peakMemoryBytes=%s, cpuTimeMillis=%s, processedBytes=%s, scanSize=%s]", user, queryHistory.getId(), uuid, queryId, peakMemoryBytes, cpuTimeMillis, processedBytes, scanSize));

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
                String message = ex.getMessage().trim();
                log.error(String.format("%s failed to close[id=%d, uuid=%s, queryId=%s, errCode=%d] when querying: %s", user, queryHistory.getId(), uuid, queryId, code, CommonUtil.printStackTraceToString(ex)));
                if (0 == code) {
                    code = 414;
                    response.put("code", code);
                    response.put("message", message);
                    response.put("data", errData);
                }
            }
        }

        if (0 != code && null != response.get("message")) {
            HashMap<String, String> detailErr = getErrorInfo(response.get("message").toString());
            response.put("errorType", detailErr.get("errorType"));
            response.put("errorZh", detailErr.get("errorZh"));
            String simpleErr = stripStackInfo(response.get("message").toString());
            response.put("message", simpleErr);
            /*if (detailErr.get("errorZh").isEmpty()) {
                response.put("errorZh", simpleErr);
                response.put("message", "");
            } else {
                response.put("message_in", simpleErr);
                response.put("message", "");
            }*/
        }
        return response;
    }

    @Override
    @Async
    public HashMap<String, Object> cancel(String uuid, String user, String tenantName) throws
            IOException, InterruptedException {
        InfTraceContextHolder.get().setTenantName(tenantName);
        log.info(String.format("%s start to cancel task[uuid=%s]", user, uuid));
        HashMap<String, Object> result = new HashMap<>();
        QueryHistory queryHistory = null;

        int count = 0;
        for (int i = 0; i < cancelCount; ++i) {
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

        QueryHistory temp = queryHistoryMapper.selectByUuid(uuid);
        if (temp.getStatus() != 2) {
            queryHistory.setStatus(2);
            queryHistory.setStatusZh("已取消");
            update(queryHistory);
        }
        String ssID = queryHistory.getSessionId();
        if (null == ssID) {
            ssID = queryHistory.getQueryId();
        }
        String urlCancel = getGatewayHttpDomain() + ssID;
        if (GatewayUtil.cancelQuery(urlCancel, user)) {
            data.put("status", 2);
            data.put("message", "已取消");
            result.put("code", 0);
            result.put("data", data);
            log.info(String.format("%s cancel task[id=%d, uuid=%s, query_id=%s] successfully",
                    user, id, uuid, queryHistory.getQueryId()));
        } else {
            data.put("status", 22);
            data.put("message", String.format("[id=%d]", id));
            result.put("code", 0);
            result.put("data", data);
            log.error(String.format("%s failed to cancel task[id=%d, uuid=%s, query_id=%s]",
                    user, id, uuid, queryHistory.getQueryId()));
        }
        return result;
    }

    @Override
    public HashMap<String, String> getQueryId(String uuid) {
        QueryHistory queryHistory = super.getByUuid(uuid);
        HashMap<String, String> result = new HashMap<>();
        result.put("query_id", queryHistory.getQueryId());
        return result;
    }

    @Override
    public ResponseEntity<Object> download(String uuid, String user, String userInfo, String id, QueryHistory queryHistory) throws Exception {
        log.info(String.format("%s start to write file to web when download csv[uuid=%s, id=%s]", user, uuid, id));

        String path = downloadToNative(uuid, user, userInfo, queryHistory, "csv");
        File file = new File(path);
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

        return ok.body(resource);
    }

    @Override
    public ResponseEntity<Object> downloadPdf(String uuid, String user, String userInfo, String id, QueryHistory queryHistory) throws Exception {
        try {
            String root = System.getProperty("user.dir");
            String fileDir = root + File.separator + "data" + File.separator + "csv";
            log.info(String.format("%s start to write file to web when downloadPdf[uuid=%s, id=%s]", user, uuid, id));
            String filePdf = fileDir + File.separator + id + ".pdf";

            String csvFile = downloadToNative(uuid, user, userInfo, queryHistory, "pdf");
            CommonUtil.transCsv2Pdf(csvFile, filePdf);

            Path pdfFilePath = Paths.get(filePdf);
            byte[] pdfBytes = Files.readAllBytes(pdfFilePath);
            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", id + ".pdf");
            headers.add("Cache-Control", "no-cache,no-store,must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            ResponseEntity.BodyBuilder ok = ResponseEntity.ok()
                    .headers(headers);

            return ok.body(pdfResource);
        } catch (Exception e) {
            log.error(String.format("[uuid=%s, user=%s]failed to download pdf:%s", uuid, user, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(e.getMessage(), e.getMessage());
        }
    }

    @Override
    public ArrayList<String> downloadLink(String uuid, String user, String userInfo)  throws Exception {
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            String message = String.format("There is no uuid[%s] info when download", uuid);
            log.error(String.format("[user=%s] failed to downloadLink csv: %s", user, message));
            throw new Exception(message);
        }

        String url = getGatewayHttpDomain();
        String ssID = queryHistory.getSessionId();
        if (null == ssID) {
            ssID = queryHistory.getQueryId();
        }
        url += ssID + "/signature";
        String outputPath = getStoragePath(queryHistory.getTenantId(), queryHistory.getRegion(), userInfo);
        return GatewayUtil.downloadLink(url, user, outputPath, queryHistory.getRegion(), queryHistory.getOperationId());
    }

    @Override
    public HashMap<String, Object> historyData(String uuid, CurrentUser currentUser) throws Exception {
        String user = currentUser.getUserName();
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
            String simpleErr = stripStackInfo(data.get("message").toString());
            data.put("message", simpleErr);
            /*if (detailErr.get("errorZh").isEmpty()) {
                data.put("errorZh", simpleErr);
                data.put("message", "");
            } else {
                data.put("message_in", simpleErr);
                data.put("message", "");
            }*/
            response.put("code", 0);
            response.put("data", data);
            return response;
        }

        String querySql = queryHistory.getQuerySql();
        String engine = queryHistory.getEngine();

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

        String url = "";
        String sql = "";
        String username = "";
        String password = "";
        String region = queryHistory.getRegion();
        Properties properties = new Properties();

        GWProperties gwProperties = new GWProperties(currentUser, gatewayUrl, querySql);
        if (engine.startsWith("spark")) {
            gwProperties.setBatchType("spark");
            gwProperties.setProperty("kyuubi.engine.type", "SPARK_SQL");
            gwProperties.setProperty("kyuubi.session.commands.tags", "type:spark-submit-sql-ds");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getSpark_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
        } else if (engine.startsWith("presto")) {
            gwProperties.setBatchType("presto");
            gwProperties.setProperty("kyuubi.engine.type", "TRINO");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getTrino_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
        } else if (engine.startsWith("mysql")) {
            Meta metaMysql = metaMapper.listByKey(engine);
            gwProperties.setBatchType("mysql");
            gwProperties.setProperty("kyuubi.engine.type", "JDBC");
            gwProperties.setProperty("kyuubi.engine.jdbc.connection.url", metaMysql.getEngineUrl());
            gwProperties.setProperty("kyuubi.engine.jdbc.connection.user", metaMysql.getUsername());
            gwProperties.setProperty("kyuubi.engine.jdbc.connection.password", metaMysql.getPassword());
            gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "Mysql8ConnectionProvider");
            properties.setProperty("kyuubi.engine.jdbc.connection.properties", "useSSL=false");
        }

        ResultSet rs = null;
        Statement statement = null;
        Connection connection = null;
        try {
            log.info(String.format("[user=%s] start to connect url[%s] when historical query data[id=%d, uuid=%s]", user, url, queryHistory.getId(), uuid));
            url = gwProperties.getUrlWithParams();
            sql = gwProperties.getSqlWithoutConf().get(0);
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
                }
                if (connection != null) {
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
    public Map<String, Object> queryLogText(String uuid, int from, int size, String user) throws Exception {
        log.info(String.format("%s start to get query log text %s", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = null;
        queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null) {
            int code = 41;
            String message = String.format("There is no valid uuid[%s] when get query log", uuid);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        String url = getGatewayHttpDomain();
        String ssID = queryHistory.getSessionId();
        if (null == ssID) {
            ssID = queryHistory.getQueryId();
        }
        url += ssID + "/localLog";
        return GatewayUtil.queryLog(url, user, from, size);
    }

    @Override
    public HashMap<String, Object> getFileSize(String uuid, String user, String userInfo) {
        log.info(String.format("%s start to get file size %s", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = null;
        queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null) {
            int code = 41;
            String message = String.format("There is no valid uuid[%s] when get file size", uuid);
            log.error(String.format("[%s]%s", user, message));
            response.put("code", code);
            response.put("message", message);
            return response;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("fileSize", fileSize(queryHistory.getTenantId(), queryHistory.getSessionId(), user, queryHistory.getRegion(), userInfo));
        response.put("code", 0);
        response.put("data", data);
        return response;
    }

    private void setBom(String src, String dst, String user, String uuid) throws IOException {
        String bom = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);
        FileChannel destAccessFileChannel = null;
        try {
            // 映射原文件到内存
            RandomAccessFile srcRandomAccessFile = new RandomAccessFile(src, "r");
            FileChannel srcAccessFileChannel = srcRandomAccessFile.getChannel();
            long srcLength = srcAccessFileChannel.size();
            MappedByteBuffer srcMap = srcAccessFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, srcLength);

            File fileDst = new File(dst);
            if (fileDst.isFile() && fileDst.exists()) {
                fileDst.delete();
            }
            // 映射目标文件到内存
            RandomAccessFile destRandomAccessFile = new RandomAccessFile(dst, "rw");
            destAccessFileChannel = destRandomAccessFile.getChannel();
            long destLength = srcLength + bom.getBytes().length;
            MappedByteBuffer destMap = destAccessFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, destLength);

            // 开始文件追加 : 先添加头部内容，再添加原来文件内容
            destMap.position(0);
            destMap.put(bom.getBytes());
            destMap.put(srcMap);
        } catch (IOException e) {
            String message = String.format("[user=%s, uuid=%s]failed to set bom: %s", user, uuid, e.getMessage());
            log.error(String.format("[user=%s, uuid=%s]failed to set bom: %s", user, uuid, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(message, message);
        } finally {
            if (destAccessFileChannel != null) {
                destAccessFileChannel.close();
            }
        }
    }

    @Override
    public String downloadToNative(String uuid, String user, String userInfo, QueryHistory queryHistory, String type) throws Exception {
        ArrayList<String > listFile = downloadLink(uuid, user, userInfo);
        if (null == listFile || listFile.isEmpty()) {
            String message = "failed to get download link";
            throw new ServiceException(message, message);
        }

        String root = System.getProperty("user.dir");
        String fileDir = root + File.separator + "data" + File.separator + "csv";
        log.info(String.format("[uuid=%s, user=%s, type=%s]start to write file to web when downloadToNative", user, uuid, type));
        String fileName = fileDir + File.separator + queryHistory.getId() + "_temp.csv";
        String dst = fileDir + File.separator + queryHistory.getId() + ".csv";

        try {
            // 创建信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            // 忽略对主机名的验证
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // 设置 SSL 上下文，使用自定义 TrustManager 和 HostnameVerifier
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            if (listFile.size() == 1) {
                FileUtils.copyURLToFile(new URL(listFile.get(0)), new File(fileName));
            } else {
                ArrayList<String> paths = new ArrayList<>();
                fileDir = fileDir + File.separator + queryHistory.getId();
                File directory = new File(fileDir);
                if (directory.exists()) {
                    PathUtil.deleteDirectory(directory);
                }
                directory.mkdirs();

                ExecutorService executorService = Executors.newFixedThreadPool(CommonConstant.DOWNLOAD_THREADS);
                for (String url : listFile) {
                    String tempName = PathUtil.fileName(url, uuid);
                    paths.add(tempName);
                    String tempPath = directory + File.separator + tempName;
                    executorService.execute(new UrlDownloader(uuid, url, tempPath));
                }
                executorService.shutdown();
                try {
                    // 等待所有线程完成
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                    PathUtil.fileNameSort(paths);

                    PrintWriter writer = new PrintWriter(new FileWriter(fileName));
                    for (String path: paths) {
                        String tempFilePath = fileDir + File.separator + path;
                        Files.lines(Paths.get(tempFilePath)).forEach(writer::println);
                    }
                } catch (InterruptedException e) {
                    log.error(String.format("[uuid=%s, user=%s]failed to merge file downloadToNative: %s",
                            uuid, user, CommonUtil.printStackTraceToString(e)));
                    throw new Exception("failed to merge file " + e.getMessage());
                }
            }
            log.info(String.format("[uuid=%s, user=%s]download file successfully when downloadToNative", uuid, user));
            setBom(fileName, dst, user, uuid);
            log.info(String.format("[uuid=%s, user=%s]set bom to file successfully when downloadToNative", uuid, user));
        } catch (Exception e) {
            log.error(String.format("[uuid=%s, user=%s]There is an exception occurred while downloadToNative: %s", uuid, user, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(e.toString(), e.toString());
        }

        return dst;
    }

    static class UrlDownloader implements Runnable {
        private final String url;
        private final String uuid;
        private final String tempPath;

        public UrlDownloader(String uuid, String url, String tempPath) {
            this.url = url;
            this.uuid = uuid;
            this.tempPath = tempPath;
        }

        @Override
        public void run() {
            try {
                FileUtils.copyURLToFile(new URL(url), new File(tempPath));
            } catch (IOException e) {
                log.error(String.format("[uuid=%s, url=%s]failed to download pdf:%s", uuid, url, CommonUtil.printStackTraceToString(e)));
                throw new RuntimeException(e);
            }
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
        String domain = getGatewayTrinoHttpDomain();

        try {
            if (1 == step) {
                statusObj.put("query_id", queryHistory.getQueryId());
                return statusObj;
            }

            if (2 == step) {
                String url = String.format("%s%s?clusterTags=%s", domain, query_id,
                        URLEncoder.encode(gatewayConfig.getTrino_cluster_tags().get(region)));
                String resInfo = CommonUtil.httpResult(url, true, null, null, uuid);
                log.info(String.format("[uuid=%s]The result of statsInfo is:%s", uuid, resInfo));

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
        } catch (Exception e) {
            log.error(String.format("[uuid=%s]There is an exception occurred while parse stats info: %s",
                    uuid, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(e.getMessage(), e.getMessage());
        }

        return statusObj;
    }

    @Override
    public boolean fromOlap(String uuid) {
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            return false;
        }
        if (null != queryHistory.getFromOlap()) {
            return queryHistory.getFromOlap();
        }
        String regex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
        boolean isOlap = Pattern.matches(regex, queryHistory.getQueryId());
        return !isOlap;
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
                if (errorZh.equals("内部报错：服务空指针异常")) {
                    errorZh = "";
                } else {
                    for (int j = 1;;) {
                        String num = "{" + String.valueOf(j) + "}";
                        if (errorZh.contains(num)) {
                            errorZh = errorZh.replace(num, m.group(j));
                            ++j;
                        } else {
                            break;
                        }
                    }
                }
                infoMap.put("errorType", errorType);
                infoMap.put("errorZh", errorZh);
                break;
            }
        }
        return infoMap;
    }

    private String stripStackInfo(String errInfo) {
        StringBuilder params = new StringBuilder();
        String errArr[] = errInfo.split("\n");
        for (int i = 0; i < errArr.length; ++i) {
            String trimInfo = errArr[i].trim();
            if (trimInfo.startsWith("at ") || trimInfo.startsWith("Caused by")
                    || trimInfo.startsWith("... ") || trimInfo.isEmpty()) {
                continue;
            }
            trimInfo.replace("org.apache.kyuubi.KyuubiSQLException: org.apache.kyuubi.KyuubiSQLException:", "");
            trimInfo.replace("Error operating ExecuteStatement: org.apache.kyuubi.jdbc.hive.KyuubiSQLException:", "");
            params.append(trimInfo).append(";");
        }
        return params.toString();
    }

    @Async
    void saveQueryResult(int query_id, ArrayList<Map<String, Object>> result,
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

    private void downloadToCsv(String uuid, String user, File csvFile,
                               boolean updateColumnType, QueryHistory queryHistory,
                               CurrentUser currentUser, String token) throws Exception {
        log.info(String.format("%s start to download task %s", user, uuid));
        String engine = queryHistory.getEngine();
        String queryId = queryHistory.getQueryId();
        String querySql = queryHistory.getQuerySql();
        String region = queryHistory.getRegion();

        String username = "";
        String password = "";
        Properties properties = new Properties();
        GWProperties gwProperties = new GWProperties(currentUser, gatewayUrl, querySql);
        if (engine.startsWith("spark")) {
            gwProperties.setBatchType("spark");
            gwProperties.setProperty("kyuubi.engine.type", "SPARK_SQL");
            gwProperties.setProperty("kyuubi.session.commands.tags", "type:spark-submit-sql-ds");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getSpark_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
        } else if (engine.startsWith("presto")) {
            gwProperties.setBatchType("presto");
            gwProperties.setProperty("kyuubi.engine.type", "TRINO");
            gwProperties.setProperty("kyuubi.session.cluster.tags", gatewayConfig.getTrino_cluster_tags().get(region));
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("SSL", "false");
        } else {
            Meta metaInfo = GatewayUtil.getDataSourceInfo(deUrl, engine, token, "default", gwProperties);
            if (metaInfo.getEngineType().equals("clickhouse")) {
                gwProperties.setBatchType("clickhouse");
                gwProperties.setProperty("kyuubi.engine.type", "JDBC");
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.url", metaInfo.getEngineUrl());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.user", metaInfo.getUsername());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.password", metaInfo.getPassword());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "ClickHouse2ConnectionProvider");
            } else {
                gwProperties.setBatchType("mysql");
                gwProperties.setProperty("kyuubi.engine.type", "JDBC");
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.url", metaInfo.getEngineUrl());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.user", metaInfo.getUsername());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.password", metaInfo.getPassword());
                gwProperties.setProperty("kyuubi.engine.jdbc.connection.provider", "Mysql8ConnectionProvider");
            }
        }

        ResultSet rs = null;
        Statement statement = null;
        Connection connection = null;
        BufferedWriter csvWriter = null;
        try {
            String sql = gwProperties.getSqlWithoutConf().get(0);
            String url = gwProperties.getUrlWithParams();
            log.info(String.format("%s start to connect url[%s] when downloading %s", user, url, uuid));
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);

            log.info(String.format("%s start to generate empty file when downloading %s", user, uuid));
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
                    csvFile), StandardCharsets.UTF_8), 1024);  // UTF-8 -- GB2312 StandardCharsets.UTF_8
            csvWriter.write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8));

            List<Object> column = new ArrayList<>();
            ArrayList<Map<String, String>> type = new ArrayList<>();
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
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
                if (null != rs) {
                    rs.close();
                }
                statement.close();
                connection.close();
            } catch (Exception ex) {
                log.error(String.format("%s failed to close [%s] file or connection when downloading: %s", user, uuid, CommonUtil.printStackTraceToString(ex)));
            }
        }
    }

    private void writeRow(List<Object> row, BufferedWriter csvWriter) throws IOException {
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

    private String getGatewayHttpDomain() {
        String[] resArr1 = gatewayUrl.split("//");
        String[] resArr2 = resArr1[1].split(":");
        return "http://" + resArr2[0] + "/api/v1/batches/";
    }

    private String getGatewayTrinoHttpDomain() {
        String[] resArr1 = gatewayUrl.split("//");
        String[] resArr2 = resArr1[1].split(":");
        return "http://" + resArr2[0] + ":10999/v1/statement/ui/api/query/state/";
    }

    private String fileSize(int tenantId, String query_id, String user,
                            String region, String userInfo) {
        try {
            String url = getGatewayHttpDomain();
            url += query_id + "/dataSize";
            String outputPath = getStoragePath(tenantId, region, userInfo);
            if (outputPath.isEmpty()) {
                return "0";
            }
            return GatewayUtil.fileSize(url, user, outputPath, region);
        } catch (Exception e) {
            log.error(String.format("%s failed to get result file size: %s",
                    user, e.getMessage()));
            return "0";
        }
    }

    private String getStoragePath(int tenantId, String userRegion, String userInfo) {
        try {
            String url = urlClusterManager + "/cluster-service/cloud/resource/search?pageNum=1&pageSize=100";
            log.info(String.format("[userRegion=%s]to get storage: url=%s", userRegion, url));
            String resInfo = ClusterManagerUtil.getClusterManagerInfo(url, userInfo);
            log.info(String.format("[userInfo=%s]to get storage: %s", userRegion, resInfo));
            if (StringUtils.isEmpty(resInfo)) {
                throw new ServiceException(BaseResponseCodeEnum.CLUSTER_NOT_AVAIL);
            }
            JSONObject content = JSON.parseObject(resInfo);
            JSONObject data = content.getJSONObject("data");
            if (data == null || data.isEmpty()) {
                throw new ServiceException(content.getString("message"), content.getString("message"));
            }
            JSONArray list = data.getJSONArray("list");
            for (int i = 0; i < list.size(); ++i) {
                JSONObject tenant = list.getJSONObject(i);
                String provider = tenant.getString("provider");
                String region = tenant.getString("region");
                String cur_region = provider + "_" + region;
                if (!cur_region.equalsIgnoreCase(userRegion)) {
                    log.info(String.format("%s get storage not match: %s", userRegion, cur_region));
                    continue;
                }
                return tenant.getString("storage");
            }
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while parse cluster info: %s",
                    CommonUtil.printStackTraceToString(e)));
            throw e;
        }
        return "";
    }
}
