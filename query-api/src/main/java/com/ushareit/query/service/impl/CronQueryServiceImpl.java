package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.CronQuery;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.mapper.CronQueryMapper;
import com.ushareit.query.mapper.UserMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.CronQueryService;
import com.ushareit.query.service.GatewayService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import io.prestosql.jdbc.$internal.okhttp3.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Setter
public class CronQueryServiceImpl extends AbstractBaseServiceImpl<CronQuery> implements CronQueryService {
    @Value("${de.gateway}")
    private String gatewayUrl;

    @Value("${qe.qe_url}")
    private String qeUrl;

    @Resource
    private CronQueryMapper cronQueryMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public CrudMapper<CronQuery> getBaseMapper() {
        return cronQueryMapper;
    }

    @Autowired
    private GatewayService gatewayService;

    @Override
    public Map<String, Object> create(String params, String token, CurrentUser curUser) throws IOException {
        // 身份认证参数
        String tenantName = curUser.getTenantName();
        String userName = curUser.getUserName();
        String groupIds = curUser.getGroupIds();
        String currentGroup = curUser.getGroupName();
        String groupName = curUser.getGroupName();
        String groupUuid = curUser.getGroupUuid();
        InfTraceContextHolder.get().setTenantName(tenantName);

        // 解析前端传来的参数Ï
        HashMap<String, Object> paramsMap = JSON.parseObject(params, HashMap.class);
        String taskName = (String) paramsMap.get("taskName");
        String schedule = (String) paramsMap.get("schedule");
        String email = (String) paramsMap.get("email");
        String startTime = (String) paramsMap.get("startTime");
        String endTime = (String) paramsMap.get("endTime");
        String originSql = (String) paramsMap.get("originSql");
        String engine = (String) paramsMap.get("engine");
        String region = (String) paramsMap.get("region");
        String catalog = (String) paramsMap.get("catalog");
        String database = (String) paramsMap.get("database");

        // 拼接传给DE的参数
        Map<String, Object> deParamsMap = new HashMap<>();
        deParamsMap.put("invokingStatus", true);
        deParamsMap.put("templateCode", "QueryEdit");
        deParamsMap.put("content", originSql);
        deParamsMap.put("name", taskName);
        deParamsMap.put("description", "");
        deParamsMap.put("dependTypes", new ArrayList<>());
        deParamsMap.put("eventDepends", new ArrayList<>());
        deParamsMap.put("inputDataset", new ArrayList<>());
        deParamsMap.put("outputDataset", new ArrayList<>());

        Map<String, Object> runtimeConfig = new HashMap<>();
        runtimeConfig.put("sqlType", "edit");
        runtimeConfig.put("sourceRegion", region);
        runtimeConfig.put("startDate", startTime);
        runtimeConfig.put("endDate", endTime);
        runtimeConfig.put("emails", email);
        runtimeConfig.put("owner", userName);
        runtimeConfig.put("collaborators", new ArrayList<>());
        List<String> dsGroups = new ArrayList<>(Arrays.asList(groupIds.split(",")));
        runtimeConfig.put("dsGroups", dsGroups);
        runtimeConfig.put("group", "");
        runtimeConfig.put("resourceLevel", "standard");
        runtimeConfig.put("executionTimeout", 0);
        runtimeConfig.put("retries", 3);
        runtimeConfig.put("maxActiveRuns", 3);
        runtimeConfig.put("clusterSla", "normal");
        runtimeConfig.put("acrossCloud", "common");
        runtimeConfig.put("batchParams", "");
        runtimeConfig.put("lifecycle", "Ec2spot");
        runtimeConfig.put("source", "task");
        runtimeConfig.put("cost", new ArrayList<>());
        if (tenantName.equals("admin")) {
            runtimeConfig.put("targetDB", "query_bi");
        } else {
            runtimeConfig.put("targetDB", "query_bi_" + tenantName);
        }
        deParamsMap.put("runtimeConfig", runtimeConfig);

        Map<String, Object> triggerParam = new HashMap<>();
        triggerParam.put("outputGranularity", schedule);
        triggerParam.put("type", "data");
        triggerParam.put("isIrregularSheduler", 1);
        deParamsMap.put("triggerParam", triggerParam);
        deParamsMap.put("userGroups", groupName);
        JSONObject jsonObject = new JSONObject(deParamsMap);
        String jsonString = jsonObject.toString();
        log.info(jsonString);

        // 调用DE接口
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();
        String createUrl = gatewayUrl + "/task/addAndStart";
        MediaType mediaType = MediaType.parse("application/json");
        String deParams = JSONObject.toJSONString(deParamsMap);
        log.info(String.format("%s create DEtask send %s", userName, deParams));
        RequestBody body = RequestBody.create(mediaType, deParams);
        Request request = new Request.Builder().url(createUrl).method("POST", body)
                .addHeader("Authentication", token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Groupid", groupIds)
                .addHeader("Uuid", groupUuid)
                .addHeader("Currentgroup", currentGroup)
                .build();

        // 解析响应结果
        Map<String, Object> response = new HashMap<>();
        try {
            Response deResponse = client.newCall(request).execute();
            String deResponseBody = deResponse.body().string();
            log.info(String.format("%s create DEtask got %s", userName, deResponseBody));
            JSONObject jsonObj = JSONObject.parseObject(deResponseBody);
            Integer deCode = jsonObj.getInteger("code");
            if (deCode == 0) {
                Integer taskId = (Integer) jsonObj.getJSONObject("data").get("id");
                LocalDateTime createTime = LocalDateTime.now();
                String decodeSql = URLDecoder.decode(new String(Base64.getDecoder().decode(originSql.getBytes())));
                cronQueryMapper.insertHistory(taskId, taskName, schedule, email, startTime, endTime, decodeSql, userName, groupUuid, engine, region, catalog, database, 1, createTime);

                // 启动任务
                RequestBody startBody = RequestBody.create(mediaType, "");
                String startTaskUrl = gatewayUrl + "/task/start?id=" + taskId;
                Request startTaskRequest = new Request.Builder().url(startTaskUrl)
                        .method("PATCH", startBody)
                        .addHeader("Authentication", token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Groupid", groupIds)
                        .addHeader("Uuid", groupUuid)
                        .addHeader("Currentgroup", currentGroup)
                        .build();
                Response startResponse = client.newCall(startTaskRequest).execute();
                String responseStr = startResponse.body().string();
                log.info(responseStr);

                response.put("message", "任务创建成功，taskId是" + taskId);
                response.put("code", 0);
            } else {
                String message = jsonObj.getString("message");
                response.put("message", "任务创建失败，原因：" + message);
                response.put("code", 500);
            }
        } catch (Exception e) {
            String errorInfo = String.valueOf(e);
            log.error(errorInfo);
            response.put("message", "任务创建失败" + errorInfo);
            response.put("code", 500);
        }
        return response;
    }

    @Override
    public Map<String, Object> execute(String params, String token, CurrentUser curUser, String userInfo) throws SQLException, ParseException, IOException {
        HashMap<String, Object> paramsMap = JSON.parseObject(params, HashMap.class);
        Integer taskId = (Integer) paramsMap.get("taskId");
        String sql = (String) paramsMap.get("querySql");
        String querySql = URLDecoder.decode(new String(Base64.getDecoder().decode(sql.getBytes())));
        paramsMap.put("querySql", querySql);
        // 获取任务参数
        CronQuery taskInfo = cronQueryMapper.selectTaskInfo(taskId);
        String engine = taskInfo.getEngine();
        String region = taskInfo.getRegion();
        String catalog = taskInfo.getCatalog();
        String database = taskInfo.getDb();
        String uuid = UUID.randomUUID().toString();
        paramsMap.put("engine", engine);
        paramsMap.put("region", region);
        paramsMap.put("catalog", catalog);
        paramsMap.put("database", database);
        paramsMap.put("uuid", uuid);
        paramsMap.put("querySqlParam", "");
        paramsMap.put("param", "{}");
        paramsMap.put("isDatabend", 0);
        paramsMap.put("isSysCall", 1);
        paramsMap.put("curUser", curUser);
        String qeParams = JSONObject.toJSONString(paramsMap);

        // 补充调用所需参数
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, qeParams);
        String curUserStr = JSON.toJSONString(curUser);
        String tenantName = curUser.getTenantName();
        String userName = curUser.getUserName();
        String groupIds = curUser.getGroupIds();
        String currentGroup = curUser.getGroupName();
        String groupName = curUser.getGroupName();
        String groupUuid = curUser.getGroupUuid();
        log.info(String.format("%s create QEtask send %s", userName, qeParams));

        // 调用QE接口
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();
        String qeExeUrl = "http://127.0.0.1:8088/query/execute";
        Request request = new Request.Builder().url(qeExeUrl)
                .method("POST", body)
                .addHeader("Authentication", "")
                .addHeader("datacake_token", token)
                .addHeader("Content-Type", "application/json")
                .addHeader("current_login_user", curUserStr)
                .addHeader("Groupid", groupIds)
                .addHeader("Uuid", groupUuid)
                .addHeader("Currentgroup", currentGroup)
                .build();

        // 解析响应结果
        Map<String, Object> response = new HashMap<>();
        try {
            Response qeResponse = client.newCall(request).execute();
            String qeResponseBody = qeResponse.body().string();
            log.info(String.format("%s create QEtask got %s", userName, qeResponseBody));
            JSONObject jsonObj = JSONObject.parseObject(qeResponseBody);
            Integer code = (Integer) jsonObj.get("code");
            String codeStr = (String) jsonObj.get("codeStr");
            if (code == 0) {
                JSONObject dataObj = (JSONObject) jsonObj.get("data");
                Integer status = (Integer) dataObj.get("status");

                // 生成结果文件下载链接
                List listFile =  gatewayService.downloadLink(uuid, userName, userInfo);
                String downloadUrl = null;
                if (null == listFile || listFile.isEmpty()) {
                    log.info(String.format("%s 获取下载链接失败", uuid));
                } else {
                    downloadUrl = listFile.get(0).toString();
                }
                response.put("codeStr", codeStr);
                response.put("code", 0);
                response.put("downloadUrl", downloadUrl);
                response.put("status", status);
            } else {
                String message = (String) jsonObj.get("message");
                String errorType = (String) jsonObj.get("errorType");
                String errorZh = (String) jsonObj.get("errorZh");
                response.put("codeStr", codeStr);
                response.put("code", 500);
                response.put("message", message);
                response.put("errorType", errorType);
                response.put("errorZh", errorZh);
            }
        } catch (Exception e) {
            String errorInfo = String.valueOf(e);
            log.error("任务执行失败：" + errorInfo);
            response.put("message", "任务执行失败：" + errorInfo);
            response.put("code", 500);
        }
        return response;
    }

    @Override
    public PageInfo<CronQuery> history(Integer pageNum, Integer pageSize, String params, CurrentUser curUser, String userInfo) {
        HashMap<String, Object> paramsMap = JSON.parseObject(params, HashMap.class);
        Integer taskId = (Integer) paramsMap.get("taskId");
        String originSql = (String) paramsMap.get("originSql");
        String userName = (String) paramsMap.get("userName");
        String taskName = (String) paramsMap.get("taskName");
        PageHelper.startPage(pageNum, pageSize);

        // 获取用户信息：是否管理员？哪个用户组？
        String curUserName = curUser.getUserName();
        String curUserGroup = curUser.getGroupUuid();
        log.info("userInfo是：" + userInfo);
        HashMap<String, Object> userInfoMap = JSON.parseObject(userInfo, HashMap.class);
        Boolean isAdmin = (Boolean) userInfoMap.get("admin");
        String owner = null;
        // 如果是管理员，则展示所有任务，否则只展示owner名下的任务
        if(isAdmin!=null && !isAdmin){
            owner = curUserName;
        }
        log.info("isAdmin值是：" + isAdmin + ", curUserName值是：" + curUserName + "，owner值是：" + owner);
        List<CronQuery> pageRecord = null;
        pageRecord = cronQueryMapper.selectHistory(taskId, originSql, userName, taskName, owner, curUserGroup);
        return new PageInfo<>(pageRecord);
    }

    @Override
    public Map<String, Object> setParam(PageInfo<CronQuery> queryHistoryList) {
        ArrayList<Object> queryHistory = new ArrayList<>();
        List<CronQuery> pageList = queryHistoryList.getList();
        Map<String, Object> pageObject = JSON.parseObject(JSON.toJSONString(queryHistoryList));

        for (int i = 0; i < pageList.size(); i++) {
            Map<String, Object> data = JSON.parseObject(JSON.toJSONString(pageList.get(i)));
            if (data.get("param") != null) {
                data.put("param", JSONObject.parseObject(data.get("param").toString()));
            }
            queryHistory.add(data);
        }
        pageObject.put("list", queryHistory);
        return pageObject;
    }

    @Override
    public Map<String, Object> changeStatus(String params, String token, CurrentUser curUser) {
        HashMap<String, Object> paramsMap = JSON.parseObject(params, HashMap.class);
        Integer taskId = (Integer) paramsMap.get("taskId");
        Integer newStatus = (Integer) paramsMap.get("newStatus");

        // 准备调用参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();
        String deStatusUrl = "";
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "");

        String curUserStr = JSON.toJSONString(paramsMap.get("curUser"));
        String tenantName = curUser.getTenantName();
        String userName = curUser.getUserName();
        String groupIds = curUser.getGroupIds();
        String currentGroup = curUser.getGroupName();
        String groupName = curUser.getGroupName();
        String groupUuid = curUser.getGroupUuid();

        // 调用QE接口
        Response deResponse = null;
        Map<String, Object> response = new HashMap<>();
        try {
            // status取值0下线、1上线、2删除
            if (newStatus == 0) {
                deStatusUrl = gatewayUrl + "/task/onlineAndOffline?id=" + taskId + "&status=0&ifnotify=false";
                Request request = new Request.Builder().url(deStatusUrl)
                        .method("PUT", body)
                        .addHeader("Authentication", token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Groupid", groupIds)
                        .addHeader("Uuid", groupUuid)
                        .addHeader("Currentgroup", currentGroup)
                        .build();
                deResponse = client.newCall(request).execute();
            } else if (newStatus == 1) {
                deStatusUrl = gatewayUrl + "/task/start?id=" + taskId;
                Request request = new Request.Builder().url(deStatusUrl)
                        .method("PATCH", body)
                        .addHeader("Authentication", token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Groupid", groupIds)
                        .addHeader("Uuid", groupUuid)
                        .addHeader("Currentgroup", currentGroup)
                        .build();
                deResponse = client.newCall(request).execute();
            } else if (newStatus == 2) {
                deStatusUrl = gatewayUrl + "/task/new/delete?id=" + taskId + "&ifNotify=false";
                Request request = new Request.Builder().url(deStatusUrl)
                        .method("DELETE", body)
                        .addHeader("Authentication", token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Groupid", groupIds)
                        .addHeader("Uuid", groupUuid)
                        .addHeader("Currentgroup", currentGroup)
                        .build();
                deResponse = client.newCall(request).execute();
            }
            String deResponseBody = deResponse.body().string();
            log.info(String.format("%s update DE [%s] got %s", taskId, deStatusUrl, deResponseBody));
            cronQueryMapper.changeStatus(taskId, newStatus);
            response.put("message", "任务状态修改成功");
            response.put("code", 0);
        } catch (Exception e) {
            String errorInfo = String.valueOf(e);
            log.error("任务状态修改失败：" + errorInfo);
            response.put("message", "任务状态修改失败：" + errorInfo);
            response.put("code", 500);
        }
        return response;
    }
}
