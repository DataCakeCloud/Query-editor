package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.configuration.OlapConfig;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.ProbeService;
import com.ushareit.query.web.utils.CommonUtil;
import com.ushareit.query.web.utils.FileUtil;
import com.ushareit.query.web.utils.RsUtil;
import com.ushareit.query.web.utils.TimeUtil;
import io.prestosql.jdbc.PrestoResultSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.io.File;

/**
 * @author: tianxu
 * @create: 2022-06-13
 */

@Slf4j
@Service
public class ProbeServiceImpl extends AbstractBaseServiceImpl<QueryHistory>  implements ProbeService {

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private MetaMapper metaMapper;

    @Override
    public CrudMapper<QueryHistory> getBaseMapper() {
        return queryHistoryMapper;
    }

    @Value("${olap.url.aws_ue1}")
    private String awsUrl;

    @Value("${olap.url.aws_sg}")
    private String awsSGUrl;

    @Value("${olap.url.huawei_sg}")
    private String huaweiUrl;

    @Autowired
    private OlapConfig olapUrl;

    @Value("${count.display}")
    private int displayCount;

    @Value("${count.download}")
    private int downloadCount;

    @Override
    public HashMap<String, Object> probe(String uuid, String user) {
        log.info(String.format("%s start to probe data for %s", user, uuid));
        HashMap<String, Object> response = new HashMap<>();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null) {
            int code = 61;
            String message = String.format("invalid uuid[%s]", uuid);
            response.put("code", code);
            response.put("message", message);
            log.error(String.format("%s failed to probe data[uuid=%s, code=%d]: %s", user, uuid, code, message));
            return response;
        }

        String root = System.getProperty("user.dir");
        String fileDir = root + File.separator + "data" + File.separator + "csv";
        File file = new File(fileDir + File.separator + uuid + "_probe.csv");

        HashMap<String, Object> data = new HashMap<>();
        int probeStatus = -1;
        if (queryHistory.getProbeStatus() != null) {
            probeStatus = queryHistory.getProbeStatus();
        }

        if ((probeStatus == 1) && file.exists()) {
            int code = 1;
            String message = "probing";
            data.put("status", code);
            data.put("message", message);
            response.put("code", code);
            response.put("data", data);
            log.error(String.format("%s is probing data[uuid=%s, code=%d]", user, uuid, code));
        } else if (probeStatus == 2) {
            String probeData = queryHistory.getProbeResult();
            int code = 0;
            data.put("status", code);
            data.put("total", queryHistory.getTotal());
            data.put("probeData", JSONObject.parseArray(probeData));
            response.put("code", code);
            response.put("data", data);
            log.error(String.format("%s takes the initiative to completes probe[uuid=%s, code=%d]", user, uuid, code));
        } else if (probeStatus == 0 || probeStatus == -1 || (probeStatus == 1 && !file.exists())) {
            int code = 2;
            //历史数据探查
            String message = "It's historical data, which needs to be re-probed";
            data.put("status", code);
            data.put("message", message);
            response.put("code", code);
            response.put("data", data);
            log.error(String.format("%s[user=%s, uuid=%s, code=%d]", message, user, uuid, code));
        } else if (probeStatus == -2) {
            response.put("code", probeStatus);
            response.put("message", queryHistory.getProbeResult());
            log.error(String.format("probe exceptional info from DB[user=%s, uuid=%s, code=%d]: %s", user, uuid, probeStatus, queryHistory.getProbeResult()));
        } else {
            int code = 62;
            String message = "unknown error";
            response.put("code", code);
            response.put("message", message);
            log.error(String.format("There is an %s when %s is probing data[uuid=%s, code=%d]", message, user, uuid, code));
        }

        if (queryHistory.getIsProbe().equals(0)) {
            queryHistory.setIsProbe(1);
            update(queryHistory);
        }
        return response;
    }

    @Override
    public JSONObject getSample(String uuid) {
        JSONObject sample = new JSONObject();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory != null) {
            if (queryHistory.getProbeSample() != null) {
                sample = JSONObject.parseObject(queryHistory.getProbeSample());
            }
        }
        return sample;
    }

    @Override
    public HashMap<String, Object> scan(String uuid, String engine, String querySql, String user) throws SQLException{
        log.info(String.format("%s start to get scan size[uuid=%s, engine=%s]", user, uuid, engine));
        HashMap<String, Object> response = new HashMap<>();

        String url = "";
        String queryId = "";
        String sql = querySql;
        Statement statement = null;
        Connection connection = null;
        Properties properties = new Properties();

//        String group = "";
        String username = "";
        String password = "";

        log.info(String.format("%s start to get config when get scan size[uuid=%s]", user, uuid));
        String conf = String.format("--conf bdp-query-request-id=%s\n", uuid);
        if (engine.startsWith("presto")) {
            sql = conf + "explain analyze " + sql;
        } else {
            sql = conf + String.format("--conf bdp-query-engine=%s\nexplain analyze %s", engine, querySql);
        }

        List<Account> account = accountMapper.listAll();
//        JSONObject groupAccount = CommonUtil.getUserGroup(user, account, adminUsername, adminPassword);
//        group = groupAccount.getString("group");
//        username = groupAccount.getString("username");
//        password = groupAccount.getString("password");
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
//            log.error(String.format("%s when get scan size[uuid=%s]: %s", message, uuid, response.toString()));  //todo: del response
//            return response;
//        }

        properties.setProperty("user", username);
        if (engine.equals("presto_huawei")) {
            url = huaweiUrl;
        } else {
            if (engine.equals("presto_aws_sg")) {
                url = awsSGUrl;
            } else if (engine.startsWith("presto") && !engine.equals("presto_aws")) {
                String region = engine.split("_")[1];
                url = olapUrl.getUrl().get(region);
            } else {
                url = awsUrl;
            }
            properties.setProperty("password", password);
            properties.setProperty("SSL", "true");
        }

        ResultSet rs = null;
        long processedBytes = 0;

        int id = 0;
        int code = -1;
        boolean b = engine.startsWith("presto") || engine.startsWith("ares");
        try {
            log.info(String.format("[user=%s]start to connect url[%s] when get scan size[uuid=%s]", user, url, uuid));
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);
            log.info(String.format("%s start to retrieve data when get scan size[uuid=%s]", user, uuid));
            queryId = rs.unwrap(PrestoResultSet.class).getQueryId();  //running时可获得

            HashMap<String, Object> data = new HashMap<>();
            JSONObject columns = RsUtil.getColumns(rs);
            ArrayList<String> column = (ArrayList<String>) columns.get("column");
            ArrayList<Map<String, Object>> result = getResult(rs, column, false);

            processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
            String scanSize = b ? FileUtil.getStringSize(processedBytes): "-";

            data.put("scanSize", scanSize);
            response.put("data", data);
            response.put("code", 0);
            log.info(String.format("%s get scan size[uuid=%s] successfully[queryId=%s]", user, uuid, queryId));
        } catch (Exception e) {
            code = 13;
            log.error(String.format("There is a stack err when %s get scan size[uuid=%s, errCode=%d]: %s", user, uuid, code, CommonUtil.printStackTraceToString(e)));

            try {
                throw e;
            } catch (SQLException | DataIntegrityViolationException ex) {
                String message = "";
                if (ex.getClass() == SQLException.class) {
                    code = ((SQLException) ex).getErrorCode();
                    message = ex.getMessage().trim();
                    log.error(String.format("There is a SQLException when %s get scan size[uuid=%s, originalCode=%d]: %s", user, uuid, code, message));
                    if (code == 0 && message.equals("Error executing query")) {
                        code = 15;
                        message = String.format("failed to retrieve data: %s", retrieveSQLExceptionCause((SQLException) ex));
                        log.error(String.format("%s failed to retrieve data when get scan size[uuid=%s, queryId=%s, errCode=%d]: %s", user, uuid, queryId, code, message));
                    }
                } else if (ex.getClass() == DataIntegrityViolationException.class) {
                    code = 1406;
                    message = ex.getCause().toString().trim();
                    log.error(String.format("%s failed to save sql when get scan size[uuid=%s, queryId=%s, errCode=%d]: %s", user, uuid, queryId, code, ((DataIntegrityViolationException) ex).getMessage()));
                } else {
                    message = ex.getMessage().trim();
                }
                log.error(String.format("%s failed to query[uuid=%s, queryId=%s, errCode=%d]: %s", user, uuid, queryId, code, message));
                message = String.format("[uuid=%s]%s", uuid, message);

                code = code>0 ? code: 999;
                response.put("code", code);
                response.put("message", message);

                log.error(String.format("%s failed to query[uuid=%s, code=%d], the result returned to the front end is: %s", user, uuid, code, response.toString()));
            }
        } finally {
            if (rs != null) {
                if (code != -1) {
                    processedBytes = rs.unwrap(PrestoResultSet.class).getStats().getProcessedBytes();
                    String scanSize = b ? FileUtil.getStringSize(processedBytes): "-";
                }
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (statement != null) {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException ex) {
                code = ex.getErrorCode();
                code = code>0 ? code: 14;
                String message = ex.getMessage().trim();
                log.error(String.format("%s failed to close[id=%d, uuid=%s, queryId=%s, errCode=%d] when querying: %s", user, id, uuid, queryId, code, message));
                response.put("code", code);
                response.put("message", message);
            }
        }

        return response;
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

    private ArrayList<Map<String, Object>> getResult(ResultSet rs, ArrayList<String> column, boolean history) throws SQLException {
        int count = 0;
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        while(rs.next()){
            if (count < displayCount) {
                Map<String, Object> row = new HashMap<>();
                for(int i=0; i<rs.getMetaData().getColumnCount(); i++) {
                    row.put(column.get(i).toString(), rs.getString(i+1));
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
}
