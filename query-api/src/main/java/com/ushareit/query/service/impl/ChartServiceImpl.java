package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.*;
import com.ushareit.query.configuration.BIConfig;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.*;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.ChartService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.web.utils.CommonUtil;
import com.ushareit.query.web.vo.BaseResponse;
import io.prestosql.jdbc.$internal.okhttp3.*;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import org.json.JSONString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.*;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Slf4j
@Service
@Setter
public class ChartServiceImpl extends AbstractBaseServiceImpl<Chart> implements ChartService {

    @Value("${bi.url.huawei_sg}")
    private String cloudhuaweiUrl;

    @Value("${bi.username.huawei_sg}")
    private String cloudhuaweiUsername;

    @Value("${bi.password.huawei_sg}")
    private String cloudhuaweiPassword;

    @Value("${bi.url.aws_ue1}")
    private String cloudawsUrl;

    @Value("${bi.username.aws_ue1}")
    private String cloudawsUsername;

    @Value("${bi.password.aws_ue1}")
    private String cloudawsPassword;

    @Value("${bi.url.aws_sg}")
    private String cloudawsSGUrl;

    @Value("${bi.username.aws_sg}")
    private String cloudawsSGUsername;

    @Value("${bi.password.aws_sg}")
    private String cloudawsSGPassword;

    @Autowired
    private BIConfig biConfig;

    @Value("${de.gateway}")
    private String gatewayUrl;

    @Value("${share_email.host}")
    private String emailServerHost;

    @Value("${share_email.protocol}")
    private String emailPro;

    @Value("${share_email.username}")
    private String emailNickName;

    @Value("${share_email.email}")
    private String emailName;

    @Value("${share_email.password}")
    private String emailPass;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private QueryDataMapper queryDataMapper;

    @Resource
    private DashChartMapper dashChartMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private FavorDashChartMapper favorDashChartMapper;

    @Resource
    private SharebiMapper shareMapper;

    @Resource
    private LogViewMapper logViewMapper;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Override
    public CrudMapper<Chart> getBaseMapper() { return chartMapper; }

    @Override
    public String selectByUsername(String title, String name, Integer active){
        List<String> existChart = chartMapper.selectByUsername(title, name, 1);
        if (existChart.contains(title)) {
            return "名字重复";
        }
        return "success";
    }

    @Override
    public String selectByUsernameUpdate(String title, String name, Integer active,Integer id){
        List<String> existChart = chartMapper.selectByUsernameUpdate(title, name, 1,id);
        if (existChart.contains(title)) {
            return "名字重复";
        }
        return "success";
    }
    @Override
    public Integer addChart(String shareid, HashMap<String,String> map, String token, String name) throws Exception {
        //todo:
        // 1、先添加系列信息进chart表；
        // 2、查uuid有无对应的调度任务，如果没有，加一个；
        String type = map.get("type");
        String params = map.get("param");
        String title = map.get("title");
        String describe = map.get("describe");
        String chartSql = map.get("querySql");
        String paramUuid = map.get("uuid");
        String engine = map.get("engine");
        log.info("添加chart：前端传来的content——————————");
        log.info(map.get("content"));
        Chart addChartToSet = new Chart();
        addChartToSet.setName(title);
        addChartToSet.setDescribeChart(describe);
        addChartToSet.setType(type);
        addChartToSet.setParam(params);
        addChartToSet.setQuerySql(chartSql);
        addChartToSet.setUuid(paramUuid);
        addChartToSet.setEngine(engine);
        addChartToSet.setIsActive(1);
        addChartToSet.setIsShare(0);
        addChartToSet.setIsFavorate(0);
        addChartToSet.setContent(map.get("content"));
        addChartToSet.setStatus("null");
        addChartToSet.setCreateBy(name);
        addChartToSet.setUpdateBy(name);
        addChartToSet.setCreateTime(new Timestamp(System.currentTimeMillis()));
        addChartToSet.setUpdateTime(new Timestamp(System.currentTimeMillis()));
//        List<String> existChart = chartMapper.selectByUsername(title, name, addChartToSet.getId(), 1);
//        if (existChart.contains(title)) {
//            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
//        }
        boolean addChartResult = chartMapper.addChart(addChartToSet);
        Integer id = addChartToSet.getId();
        return id;
    }

    @Override
    public String updateChart(String shareid, HashMap<String,String> map,String token,String name,String tenantName){
        //todo:
        // 1.先判断这个chart的id和参数的uuid是否一致，若一致，则与调度无关；否则走下面流程
        // 2.更新chart基本表：param，uuid，类型，名称，描述，sql，更新时间；
        // 3.查uuid有无对应的调度任务，如果没有，加一个并更新；
        // 4.查这个chart有无在dashboard里，dashboard有没有开调度，开了的话调度开启天粒度；
        try {
            log.info("开始更新chart——————————");
            JSONArray columnList = JSONArray.parseArray(map.get("columnList").toString());
            log.info("更新chart：前端传来的字段及其类型————————————");
            log.info(columnList.toString());
            String addUrl = gatewayUrl+"/task/add";
            Integer id = Integer.parseInt(map.get("id"));
            String type =map.get("type");
            String params = map.get("param");
            String title = map.get("title");
            String describe = map.get("describe");
            String chartSql = map.get("querySql");
            String paramUuid = map.get("uuid");
            String engine = map.get("engine");
            log.info("更新chart：前端传的uuid——————————");
            log.info(paramUuid);
            LocalDateTime time = LocalDateTime.now();
            List<Map> partitions = new ArrayList<>();
            String uuid = chartMapper.selectUuidById(id,name);
            log.info("更新chart：原始chart的uuid——————————");
            log.info(uuid);
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            //获取当前用户的所有用户组
            String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
            Request requestGetGroup = new Request.Builder()
                    .url(getGroupUrl)
                    .method("GET", null)
                    .addHeader("Authentication", token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response responseGetGroup = client.newCall(requestGetGroup).execute();
            String responseGetGroupBody = responseGetGroup.body().string();
            log.info(String.format("%s update chart get group info get %s",
                    shareid, responseGetGroupBody));
            JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
            JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
            Integer groupId = (Integer) data.get("id");
            List<Map> costList = new ArrayList<>();
            Map cost = new HashMap();
            cost.put("key", groupId);
            cost.put("value", 100);
            costList.add(cost);
            if (uuid.equals(paramUuid)){
                boolean updateResult = chartMapper.updateResult(type, params, title, describe,
                        chartSql, engine, time, id, name);
            }else{
                //todo:查询uuid还有没有chart在用，没有的话下掉；
                List<Chart> chartuuid = chartMapper.selectListByUuid(uuid,1,id);
                if (chartuuid.size()==0){
                    QueryData qd = queryDataMapper.getQd(uuid);
                    String delUrl = gatewayUrl+"/task/new/delete?id=";
                    delUrl += qd.getDetaskid()+"&ifnotify=false";
                    RequestBody body = RequestBody.create(mediaType, "");
                    Request requestDapartment = new Request.Builder()
                            .url(delUrl)
                            .method("PUT", body)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseEditTask = client.newCall(requestDapartment).execute();
                    String responseEditBody = responseEditTask.body().string();
                    log.info("更新chart：删除DE任务信息——————————");
                    log.info(String.format("%s update chart del DE [%s] get %s",
                            shareid, delUrl, responseEditBody));
                    boolean delQueryData = queryDataMapper.del(uuid,0);
                }
                boolean updateResult = chartMapper.updateResultUuid(paramUuid, type, params, title,
                        describe, chartSql, engine, time, id, name);
                List<QueryData> uuidForDe = queryDataMapper.selectUuidForDe(paramUuid,1);
                //todo:判断sql是否有对应调度
                if (uuidForDe.size()==0){
                    //新增一个调度
                    Map addTaskValues = new HashMap<>();
                    String taskName = "bi_"+ id + "_dataset_" + shareid;
                    log.info("更新chart：DE任务名称——————————");
                    log.info(taskName);
                    String uuidName = paramUuid.replace("-","_");
                    String taskDataName = "bi_"+uuidName+"_dataset_"+shareid;
                    log.info("更新chart：DE导入mysql后mysql表名称——————————");
                    log.info(taskDataName);
                    Map runtimeConfig = new HashMap();
                    List columns = new ArrayList();
                    Map partitionsMap = new HashMap();
                    partitionsMap.put("name","bidt");
                    partitionsMap.put("value","{{ ds_nodash }}");
                    partitions.add(partitionsMap);

                    Map crontabParam = new HashMap();
                    crontabParam.put("endTime","23:59");
                    crontabParam.put("fixedTime","00:00");
                    crontabParam.put("interval",5);
                    crontabParam.put("range",new ArrayList<>());
                    crontabParam.put("startTime","00:00");

                    Map triggerParam = new HashMap();
                    triggerParam.put("crontab","00 00 * * *");
                    triggerParam.put("outputGranularity","daily");
                    triggerParam.put("type","cron");
                    triggerParam.put("crontabParam",crontabParam);
                    String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartSql).getBytes()));
                    //todo:加调度参数
                    Map typeMap = new HashMap();
                    typeMap.put("string","varchar(255)");
                    typeMap.put("int","int");
                    typeMap.put("double","double");
                    typeMap.put("tinyint","int");
                    typeMap.put("smallint","int");
                    typeMap.put("bigint","bigint");
                    typeMap.put("float","float");
                    typeMap.put("real","float");
                    typeMap.put("varchar","varchar(255)");
                    typeMap.put("boolean","boolean");
                    typeMap.put("","varchar(255)");
                    typeMap.put("integer","int");
                    for (int i = 0; i < columnList.size(); i++) {
                        Map colmap = (Map) columnList.get(i);
                        for (Object key : colmap.keySet()) {
                            Map col = new IdentityHashMap();
                            col.put(new String("columnName"), key.toString());
                            col.put(new String("columnType"), typeMap.get(colmap.get(key.toString()).toString().trim()));
                            columns.add(col);
                        }
                    }
                    log.info("更新chart：传给DE的表字段信息——————————");
                    log.info(columns.toString());
                    runtimeConfig.put("acrossCloud","common");
                    runtimeConfig.put("alertMethod",new ArrayList<>());
                    runtimeConfig.put("alertType",new ArrayList<>());
                    runtimeConfig.put("batchParams","");
                    runtimeConfig.put("clusterSla","normal");
                    runtimeConfig.put("collaborators",new ArrayList<>());
                    runtimeConfig.put("columns",columns);
                    {
                        runtimeConfig.put("connectionUrl", biConfig.getUrl().get(map.get("region")));
                        runtimeConfig.put("dbUser", biConfig.getUsername().get(map.get("region")));
                        String password = encrypt(biConfig.getPassword().get(map.get("region")));
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (map.get("region").equals("aws_ue1")){
                        runtimeConfig.put("connectionUrl",cloudawsUrl);
                        runtimeConfig.put("dbUser",cloudawsUsername);
                        String password = encrypt(cloudawsPassword);
                        runtimeConfig.put("dbPassword",password);
                    }
                    if (map.get("region").equals("aws_sg")){
                        runtimeConfig.put("connectionUrl",cloudawsSGUrl);
                        runtimeConfig.put("dbUser",cloudawsSGUsername);
                        String password = encrypt(cloudawsSGPassword);
                        runtimeConfig.put("dbPassword",password);
                    }
                    if (map.get("region").equals("huawei_sg")){
                        runtimeConfig.put("connectionUrl",cloudhuaweiUrl);
                        runtimeConfig.put("dbUser",cloudhuaweiUsername);
                        String password = encrypt(cloudhuaweiPassword);
                        runtimeConfig.put("dbPassword",password);
                    }
                    runtimeConfig.put("dsGroups",new ArrayList<>());
                    runtimeConfig.put("emails","");
                    runtimeConfig.put("endDate","2010-11-10 23:59:59");
//                    runtimeConfig.put("endDate","");
                    runtimeConfig.put("executionTimeout",0);
                    runtimeConfig.put("existTargetTable",false);
                    runtimeConfig.put("group","");
                    runtimeConfig.put("lifecycle","Ec2spot");
                    runtimeConfig.put("maxActiveRuns",1);
                    runtimeConfig.put("owner",name);
                    runtimeConfig.put("resourceLevel","standard");
                    runtimeConfig.put("partitions",partitions);
                    runtimeConfig.put("retries",1);
                    runtimeConfig.put("source","task");
                    runtimeConfig.put("sourceColumns",new ArrayList<>());
                    if (map.get("region").equals("aws_ue1")){
                        runtimeConfig.put("sourceRegion","ue1");
                    }
                    if (map.get("region").equals("aws_sg")){
                        runtimeConfig.put("sourceRegion","sg1");
                    }
                    if (map.get("region").equals("huawei_sg")){
                        runtimeConfig.put("sourceRegion","sg2");
                    }
                    runtimeConfig.put("startDate","2010-11-10 00:00:00");
//                    runtimeConfig.put("startDate","");
                    if (tenantName.equals("admin")){
                        runtimeConfig.put("targetDB", "query_bi");
                    }else {
                        String dbName = "query_bi";
                        runtimeConfig.put("targetDB",dbName + "_" + tenantName);
                    }
                    runtimeConfig.put("targetTable",taskDataName);
                    runtimeConfig.put("cost",costList);

                    addTaskValues.put("eventDepends",new ArrayList<>());
                    addTaskValues.put("dependTypes",new ArrayList<>());
                    addTaskValues.put("description","");
//                    addTaskValues.put("content",encodedSql);
                    addTaskValues.put("content",map.get("content"));
                    if (null != engine && engine.startsWith("presto")) {
                        addTaskValues.put("templateCode", "TrinoJob");
                    } else {
                        addTaskValues.put("templateCode", "Hive2Mysql");
                    }
                    addTaskValues.put("inputDataset",new ArrayList<>());
                    addTaskValues.put("outputDataset",new ArrayList<>());
                    addTaskValues.put("invokingStatus",true);
                    addTaskValues.put("name",taskDataName);
                    addTaskValues.put("runtimeConfig",runtimeConfig);
                    addTaskValues.put("triggerParam",triggerParam);

                    String valuesForAddTask = JSONObject.toJSONString(addTaskValues);
                    log.info(String.format("%s update chart add DE send %s",
                            shareid, valuesForAddTask));
                    RequestBody bodyForAddTask = RequestBody.create(mediaType,valuesForAddTask);
                    Request requestAddTask = new Request.Builder()
                            .url(addUrl)
                            .method("POST", bodyForAddTask)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseAddTask = client.newCall(requestAddTask).execute();
                    String responseAddTaskBody = responseAddTask.body().string();
                    log.info("更新chart：添加新DE任务结果——————————");
                    log.info(String.format("%s update chart add DE get %s",
                            shareid, responseAddTaskBody));
                    JSONObject jsonAddTaskObj = JSONObject.parseObject(responseAddTaskBody);
                    Integer taskId = (Integer) jsonAddTaskObj.getJSONObject("data").get("id");
                    //todo:把taskid加到query_data那个表里
                    boolean addTaskIdToQueryData = queryDataMapper.addTaskIdToQueryData(taskId,paramUuid,map.get("region").toString(),taskDataName,time, 1);
                    if (!jsonAddTaskObj.get("code").equals(0)){
                        return "添加调度失败";
                    }
                    TimeUnit.SECONDS.sleep(1);
                    //todo:上线
                    String onlineAndOffline = gatewayUrl+"/task/start?id=";
                    onlineAndOffline += taskId;
                    RequestBody body = RequestBody.create(mediaType, "");
                    Request requestOnlineTask = new Request.Builder()
                            .url(onlineAndOffline)
                            .method("PATCH",body)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
                    String responseOnlineTaskBody = responseOnlineTask.body().string();
                    log.info("更新chart：上线DE任务结果——————————");
                    log.info(String.format("%s update chart online DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
                    JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
                    if (!jsonOnlineTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",taskId)));
                    }
                List<DashChart> isInDashboard = dashChartMapper.selectByChartId(id, name,1);
                //todo:判断该chart是否在dashboard中
                if (isInDashboard.size()==0){
                    TimeUnit.SECONDS.sleep(1);
                    //todo:调度补数当前一天时间
                    LocalDate timeDate = LocalDate.now();
                    String backfill = gatewayUrl+"/task/backfill?id=";
                    backfill = backfill + taskId + "&startDate=" + timeDate + "+00:00:00" + "&endDate=" + timeDate + "+23:59:59&childIds=&isCheckDependency=true&isSendNotify=true";
                    Request requestEditTask = new Request.Builder()
                            .url(backfill)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseEditTask = client.newCall(requestEditTask).execute();
                    String responseEditTaskBody = responseEditTask.body().string();
                    log.info("更新chart：补数DE结果——————————");
                    log.info(String.format("%s update chart backfill DE [%s] get %s",
                            shareid, backfill, responseEditTaskBody));
                    JSONObject jsonEditTaskObj = JSONObject.parseObject(responseEditTaskBody);
//                    if (!jsonAddTaskObj.get("code").equals(0)){
//                        return "补数失败";
//                    }
                }else{
                    List<Integer> dashIdList = new ArrayList();
                    List<String> crontabList = new ArrayList();
                    for (DashChart dash:isInDashboard){
                        Integer dashById = dashboardMapper.selectDashById(dash.getId(), name,1);
                        dashIdList.add(dashById);
                        String crontab = dashboardMapper.getCrontab(dash.getId(),1,1);
                        crontabList.add(crontab);
                    }
                    //chart所在的所有看板中，有看板开启了天级调度
                    if (dashIdList.contains(1)){
                        Integer deTaskId = queryDataMapper.selectDeTaskId(paramUuid,1);
                        //todo：先下线任务
                        String offLine = gatewayUrl+"/task/onlineAndOffline?id=";
                        offLine = offLine+deTaskId+"&status=0&ifnotify=false";
                        RequestBody offLineBody = RequestBody.create(mediaType, "");
                        Request requestOffLineTask = new Request.Builder()
                                .url(offLine)
                                .method("PUT", offLineBody)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseOffLineTask = client.newCall(requestOffLineTask).execute();
                        String responseOffLineTaskBody = responseOffLineTask.body().string();
                        log.info("更新chart：对chart更改uuid： 先下线任务——————————");
                        log.info(String.format("%s update chart offline DE [%s] get %s",
                                shareid, offLine, responseOffLineTaskBody));
                        TimeUnit.SECONDS.sleep(1);
                        //todo：调接口把这个deTaskId的调度开启天粒度
                        String updateUrl = gatewayUrl+"/task/updateAndStart";
                        Map updateTaskValues = new HashMap<>();
                        String taskNameUpdate = "bi_"+ id + "_dataset" + shareid;
                        log.info("更新chart开启天调度：传给DE的任务名——————————");
                        log.info(taskNameUpdate);
                        String uuidNameUpdate = paramUuid.replace("-","_");
                        String taskDataNameUpdate = "bi_"+uuidNameUpdate+"_dataset_"+shareid;
                        log.info("更新chart开启天调度：DE导入mysql的数据表名——————————");
                        log.info(taskDataNameUpdate);
                        Map runtimeConfigUpdate = new HashMap();
                        List columnsUpdate = new ArrayList();
                        Map partitionsMapUpdate = new HashMap();
                        partitionsMapUpdate.put("name","bidt");
                        partitionsMapUpdate.put("value","{{ds_nodash}}");
                        partitions.add(partitionsMapUpdate);

                        Map crontabParamUpdate = new HashMap();
                        crontabParamUpdate.put("endTime","23:59");
                        crontabParamUpdate.put("fixedTime","00:00");
                        crontabParamUpdate.put("interval",5);
                        crontabParamUpdate.put("range",new ArrayList<>());
                        crontabParamUpdate.put("startTime","00:00");

                        Map triggerParamUpdate = new HashMap();
                        triggerParamUpdate.put("crontab",crontabList.get(0));
                        triggerParamUpdate.put("outputGranularity","daily");
                        triggerParamUpdate.put("type","cron");
                        triggerParamUpdate.put("crontabParam",crontabParamUpdate);
//                        String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartSql).getBytes()));
                        //todo:加调度参数
                        Map typeUpMap = new HashMap();
                        typeUpMap.put("string","varchar(255)");
                        typeUpMap.put("int","int");
                        typeUpMap.put("double","double");
                        typeUpMap.put("tinyint","int");
                        typeUpMap.put("smallint","int");
                        typeUpMap.put("bigint","bigint");
                        typeUpMap.put("float","float");
                        typeUpMap.put("","varchar(255)");
                        typeUpMap.put("integer","int");
                        for (int i = 0; i < columnList.size(); i++) {
                            Map colmap = (Map) columnList.get(i);
                            for (Object key : colmap.keySet()) {
                                Map col = new IdentityHashMap();
                                col.put(new String("columnName"), key.toString());
                                col.put(new String("columnType"), typeUpMap.get(colmap.get(key.toString()).toString().trim()));
                                columnsUpdate.add(col);
                            }
                        }
                        runtimeConfigUpdate.put("acrossCloud","common");
                        runtimeConfigUpdate.put("alertMethod",new ArrayList<>());
                        runtimeConfigUpdate.put("alertType",new ArrayList<>());
                        runtimeConfigUpdate.put("batchParams","");
                        runtimeConfigUpdate.put("clusterSla","normal");
                        runtimeConfigUpdate.put("collaborators",new ArrayList<>());
                        runtimeConfigUpdate.put("columns",columnsUpdate);
                        {
                            runtimeConfig.put("connectionUrl", biConfig.getUrl().get(map.get("region")));
                            runtimeConfig.put("dbUser", biConfig.getUsername().get(map.get("region")));
                            String password = encrypt(biConfig.getPassword().get(map.get("region")));
                            runtimeConfig.put("dbPassword", password);
                        }
                        if (map.get("region").equals("aws_ue1")){
                            runtimeConfigUpdate.put("connectionUrl",cloudawsUrl);
                            runtimeConfigUpdate.put("dbUser",cloudawsUsername);
                            String password = encrypt(cloudawsPassword);
                            runtimeConfigUpdate.put("dbPassword",password);
                        }
                        if (map.get("region").equals("aws_sg")){
                            runtimeConfigUpdate.put("connectionUrl",cloudawsSGUrl);
                            runtimeConfigUpdate.put("dbUser",cloudawsSGUsername);
                            String password = encrypt(cloudawsSGPassword);
                            runtimeConfigUpdate.put("dbPassword",password);
                        }
                        if (map.get("region").equals("huawei_sg")){
                            runtimeConfigUpdate.put("connectionUrl",cloudhuaweiUrl);
                            runtimeConfigUpdate.put("dbUser",cloudhuaweiUsername);
                            String password = encrypt(cloudhuaweiPassword);
                            runtimeConfigUpdate.put("dbPassword",password);
                        }
                        runtimeConfigUpdate.put("dsGroups",new ArrayList<>());
                        runtimeConfigUpdate.put("emails","");
//                    runtimeConfig.put("endDate","2010-11-10 23:59:59");
                        runtimeConfigUpdate.put("endDate","");
                        runtimeConfigUpdate.put("executionTimeout",0);
                        runtimeConfigUpdate.put("existTargetTable",false);
                        runtimeConfigUpdate.put("group","");
                        runtimeConfigUpdate.put("lifecycle","Ec2spot");
                        runtimeConfigUpdate.put("maxActiveRuns",1);
                        runtimeConfigUpdate.put("owner",name);
                        runtimeConfigUpdate.put("resourceLevel","standard");
                        runtimeConfigUpdate.put("partitions",partitions);
                        runtimeConfigUpdate.put("retries",1);
                        runtimeConfigUpdate.put("source","task");
                        runtimeConfigUpdate.put("sourceColumns",new ArrayList<>());
                        if (map.get("region").equals("aws_ue1")){
                            runtimeConfigUpdate.put("sourceRegion","ue1");
                        }
                        if (map.get("region").equals("aws_sg")){
                            runtimeConfigUpdate.put("sourceRegion","sg1");
                        }
                        if (map.get("region").equals("huawei_sg")){
                            runtimeConfigUpdate.put("sourceRegion","sg2");
                        }
//                    runtimeConfig.put("startDate","2010-11-10 00:00:00");
                        runtimeConfigUpdate.put("startDate","");
                        if (tenantName.equals("admin")){
                            runtimeConfig.put("targetDB", "query_bi");
                        }else {
                            String dbName = "query_bi";
                            runtimeConfig.put("targetDB",dbName + "_" + tenantName);
                        }
                        runtimeConfigUpdate.put("targetTable",taskDataNameUpdate);
                        runtimeConfigUpdate.put("cost",costList);

                        updateTaskValues.put("eventDepends",new ArrayList<>());
                        updateTaskValues.put("dependTypes",new ArrayList<>());
                        updateTaskValues.put("description","");
//                        updateTaskValues.put("content",encodedSql);
                        updateTaskValues.put("content",map.get("content"));
                        if (null != engine && engine.startsWith("presto")) {
                            addTaskValues.put("templateCode", "TrinoJob");
                        } else {
                            addTaskValues.put("templateCode", "Hive2Mysql");
                        }
                        updateTaskValues.put("inputDataset",new ArrayList<>());
                        updateTaskValues.put("outputDataset",new ArrayList<>());
                        updateTaskValues.put("invokingStatus",true);
                        updateTaskValues.put("name",taskDataNameUpdate);
                        updateTaskValues.put("runtimeConfig",runtimeConfigUpdate);
                        updateTaskValues.put("triggerParam",triggerParamUpdate);
                        updateTaskValues.put("id",deTaskId);
                        String valuesForUpdateTask = JSONObject.toJSONString(updateTaskValues);
                        log.info(String.format("%s update chart add crontab send %s",
                                shareid, valuesForUpdateTask));
                        RequestBody bodyForUpdateTask = RequestBody.create(mediaType,valuesForUpdateTask);
                        Request requestUpdateTask = new Request.Builder()
                                .url(updateUrl)
                                .method("POST", bodyForUpdateTask)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseUpdateTask = client.newCall(requestUpdateTask).execute();
                        String responseUpdateTaskBody = responseUpdateTask.body().string();
                        log.info("更新chart开启天调度：DE任务新增结果——————————");
                        log.info(String.format("%s update chart add crontab get %s",
                                shareid, responseUpdateTaskBody));
                        JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                        if (!jsonAddTaskObj.get("code").equals(0)){
                            return "开启天级调度失败";
                        }
                        TimeUnit.SECONDS.sleep(1);
                        //todo：上线任务
                        String start = gatewayUrl+"/task/start?id=";
                        start += deTaskId;
                        RequestBody startBody = RequestBody.create(mediaType, "");
                        Request requestStartTask = new Request.Builder()
                                .url(start)
                                .method("PATCH",startBody)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseStartTask = client.newCall(requestStartTask).execute();
                        String responseStartTaskBody = responseStartTask.body().string();
                        log.info("更新chart：对chart更改uuid;上线DE任务结果——————————");
                        log.info(String.format("%s update chart online DE [%s] get %s",
                                shareid, start, responseStartTaskBody));
                        JSONObject jsonStartTaskObj = JSONObject.parseObject(responseStartTaskBody);
                        if (!jsonStartTaskObj.get("code").equals(0)){
                            throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",deTaskId)));
                        }
                    }
                    else{
                        TimeUnit.SECONDS.sleep(1);
                        //todo:调度补数当前一天时间
                        LocalDate timeDate = LocalDate.now();
                        String backfill = gatewayUrl+"/task/backfill?id=";
                        backfill = backfill + taskId + "&startDate=" + timeDate + "+00:00:00" + "&endDate=" + timeDate + "+23:59:59&childIds=&isCheckDependency=true&isSendNotify=true";
                        Request requestEditTask = new Request.Builder()
                                .url(backfill)
                                .method("GET", null)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseEditTask = client.newCall(requestEditTask).execute();
                        String responseEditTaskBody = responseEditTask.body().string();
                        log.info("更新chart：补数DE结果——————————");
                        log.info(String.format("%s update chart backfill DE [%s] get %s",
                                shareid, backfill, responseEditTaskBody));
                        JSONObject jsonEditTaskObj = JSONObject.parseObject(responseEditTaskBody);
                    }
                }
            }
        }
        }catch (Exception e){
            return e.toString();
        }
        return "success";
    }

    @Override
    public String deleteChart(String name, Integer id,String token) throws IOException {
        List<DashChart> dc = dashChartMapper.seldash(id,1);
        if (dc.size()==0){
            Integer active = 0;
            LocalDateTime updateTime = LocalDateTime.now();
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            String chartUuid = chartMapper.selectUuidById(id,name);
            List<Chart> chartList = chartMapper.selectListByUuid(chartUuid,1,id);
            if (chartList.isEmpty()){
                Integer taskId = queryDataMapper.selectDeTaskId(chartUuid,1);
                String delUrl = gatewayUrl+"/task/new/delete?id=";
                delUrl += taskId+"&ifNotify=false";
                log.info("删除chart：传值DE的url——————————");
                log.info(delUrl);
//            Map delTaskValues = new HashMap<>();
//            delTaskValues.put("id",id.toString());
//            delTaskValues.put("status","0");
                RequestBody body = RequestBody.create(mediaType, "");
//            String valuesForDelTask = JSONObject.toJSONString(delTaskValues);
//            RequestBody bodyForDelTask = RequestBody.create(mediaType,valuesForDelTask);
                Request requestDapartment = new Request.Builder()
                        .url(delUrl)
                        .method("DELETE", body)
                        .addHeader("Authentication", token)
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response responseEditTask = client.newCall(requestDapartment).execute();
                String responseEditTaskBody = responseEditTask.body().string();
                log.info("删除chart：删除DE任务结果——————————");
                log.info(String.format("%s del chart del DE [%s] get %s",
                        name, delUrl, responseEditTaskBody));
                JSONObject jsonEditTaskObj = JSONObject.parseObject(responseEditTaskBody);
                boolean delQueryData = queryDataMapper.del(chartUuid,active);
            }
            boolean delChart = chartMapper.del(id,updateTime,active,name);
//            List<DashChart> dashchart = dashChartMapper.seldash(id,1);
//            if (dashchart.size()!=0){
//                boolean delDashChart = dashChartMapper.del(id,updateTime,active);
//            }
            return "success";
        }
        else {
            return "删除失败：该图表已被仪表盘引用";
        }

    }

    @Override
    public int addShareGrade(Sharebi sh, String shareeEmail) {
        try {
            String a= "";
            Sharebi share = shareMapper.selectByShare(sh.getSharer(),sh.getSharee(),sh.getType(),sh.getShareId());
            if (share==null){
                shareMapper.addNewShare(sh);
            }
            int shareID = sh.getShareId();

            String emailHost = emailServerHost;       //发送邮件的主机
            String transportType = emailPro;           //邮件发送的协议
            String fromUser = emailNickName;           //发件人名称
            String fromEmail = emailName;  //发件人邮箱
            String authCode = emailPass;    //发件人邮箱授权码
            // String toEmail = sg.getSharee() + "@ushareit.com";   //收件人邮箱
            String subject = sh.getSharer() + "通过Datacake平台分享给您一条chart页面";           //主题信息
            //初始化默认参数
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", transportType);
            props.setProperty("mail.host", emailHost);
            props.setProperty("mail.user", fromUser);
            props.setProperty("mail.from", fromEmail);
            //获取Session对象
            Session session = Session.getInstance(props, null);
            //开启后有调试信息
            session.setDebug(true);

            //通过MimeMessage来创建Message接口的子类
            MimeMessage message = new MimeMessage(session);
            //下面是对邮件的基本设置
            //设置发件人：
            //设置发件人第一种方式：直接显示：antladdie <antladdie@163.com>
            //InternetAddress from = new InternetAddress(sender_username);
            //设置发件人第二种方式：发件人信息拼接显示：蚂蚁小哥 <antladdie@163.com>
            String formName = MimeUtility.encodeWord(fromUser) + " <" + fromEmail + ">";
            InternetAddress from = new InternetAddress(formName);
            message.setFrom(from);

            //设置收件人：
            InternetAddress to = new InternetAddress(shareeEmail);
            message.setRecipient(Message.RecipientType.TO, to);

            //设置邮件主题
            message.setSubject(subject);

            //设置邮件内容,这里我使用html格式，其实也可以使用纯文本；纯文本"text/plain"
            String link = sh.getShareUrl() + "&BIID=" + String.valueOf(shareID);;
            String content = sh.getSharer() + "通过Datacake平台分享给您一条chart页面 ，点击链接即可快速访问";
            content += "<a href=\"" + link + "\">" + link + "</a>";
            message.setContent(content, "text/html;charset=UTF-8");

            //保存上面设置的邮件内容
            message.saveChanges();

            //获取Transport对象
            Transport transport = session.getTransport();
            //smtp验证，就是你用来发邮件的邮箱用户名密码（若在之前的properties中指定默认值，这里可以不用再次设置）
            transport.connect(emailHost, fromEmail, authCode);
            //发送邮件
            transport.sendMessage(message, message.getAllRecipients()); // 发送
            transport.close();
        } catch (Exception e) {
            log.error(String.format("There is a stack err when %s share to %s : %s",
                    sh.getSharer(), sh.getSharee(), CommonUtil.printStackTraceToString(e)));
            return -1;
        }
        return sh.getShareId();
    }

    public List<Map> getListForString(String json){
        List<Object> list = JSON.parseArray(json);
        List<Map> listw = new ArrayList<Map>();
        for (Object object : list){
            Map<String,Object> ageMap = new HashMap<String,Object>();
            Map <String,Object> ret = (Map<String, Object>) object;//取出list里面的值转为map
        /*for (Entry<String, Object> entry : ret.entrySet()) {
            ageMap.put(entry.getKey());
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            listw.add(ageMap);  //添加到list集合  成为 list<map<String,Object>> 集合
        }  */
            listw.add(ret);
        }
        return listw;
    }

    @Override
    public List<Chart> getChart(Integer id){
        return chartMapper.getChart(id,1);
    }

    @Override
    @Cacheable(cacheNames = {"chart"}, key = "#tenantName+'-'+#name+'-'+#type+'-'+#pageNum+'-'+#pageSize+'-'+#title")
    public PageInfo<List> getList(String type, String tenantName, String name, Integer pageNum, Integer pageSize, String title,String token) throws IOException {
        // OkHttpClient client = new OkHttpClient().newBuilder()
        //         .build();
        // MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        Integer total = null;
        List chart = new ArrayList();
        if (type.equals("my")){
            List<Chart> chartList = chartMapper.getChartByCreat(name,1,title);
            total = chartList.size();
            for (Chart ch:chartList){
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                String uuidTime = new String();
                if (qd == null){
                    uuidTime = "null";
                }else{
                    uuidTime = date.format(qd.getCreateTime().getTime());
                }
                Map chMap = new HashMap();
                chMap.put("content",ch.getContent());
                chMap.put("createBy",ch.getCreateBy());
                chMap.put("createTime",ch.getCreateTime());
                chMap.put("describeChart",ch.getDescribeChart());
                chMap.put("id",ch.getId());
                chMap.put("isActive",ch.getIsActive());
                chMap.put("isFavorate",ch.getIsFavorate());
                chMap.put("isShare",ch.getIsShare());
                chMap.put("name",ch.getName());
                chMap.put("param",ch.getParam());
                chMap.put("querySql",ch.getQuerySql());
                chMap.put("type",ch.getType());
                chMap.put("updateBy",ch.getUpdateBy());
                chMap.put("updateTime",ch.getUpdateTime());
                chMap.put("uuid",ch.getUuid());
                chMap.put("uuid_time",uuidTime);
                chMap.put("columnList",qh.getColumnType());
                chart.add(chMap);
            }
        }
        if (type.equals("tuck")){
            List<Integer> favorChart = favorDashChartMapper.getFavorChart(name, "chart", 1);
            total = favorChart.size();
            for (Integer id:favorChart){
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsFavorate(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chart.add(chMap);
                }
            }
        }
        if (type.equals("share")){
            List<Integer> shareChart = shareMapper.getShareChart(name,"chart");
            total = shareChart.size();
            for (Integer id:shareChart){
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsShare(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chart.add(chMap);
                }
            }
        }
        if (type.equals("all")){
            List<Chart> chartList = chartMapper.getChartByCreat(name,1,title);
            List<Integer> shareChart = shareMapper.getShareChart(name,"chart");
            total = chartList.size() + shareChart.size();
            for (Chart ch:chartList){
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                String uuidTime = new String();
                if (qd == null){
                    uuidTime = "null";
                }else{
                    uuidTime = date.format(qd.getCreateTime().getTime());
                }
                Map chMap = new HashMap();
                chMap.put("content",ch.getContent());
                chMap.put("createBy",ch.getCreateBy());
                chMap.put("createTime",ch.getCreateTime());
                chMap.put("describeChart",ch.getDescribeChart());
                chMap.put("id",ch.getId());
                chMap.put("isActive",ch.getIsActive());
                chMap.put("isFavorate",ch.getIsFavorate());
                chMap.put("isShare",ch.getIsShare());
                chMap.put("name",ch.getName());
                chMap.put("param",ch.getParam());
                chMap.put("querySql",ch.getQuerySql());
                chMap.put("type",ch.getType());
                chMap.put("updateBy",ch.getUpdateBy());
                chMap.put("updateTime",ch.getUpdateTime());
                chMap.put("uuid",ch.getUuid());
                chMap.put("uuid_time",uuidTime);
                chMap.put("columnList",qh.getColumnType());
                chart.add(chMap);
            }
            for (Integer id:shareChart){
                //todo：is_share置1给前端做一个校验
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsShare(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chart.add(chMap);
                }
            }
        }
        Integer t = (int) new PageInfo<>(chart).getTotal();

        PageHelper.startPage(pageNum, pageSize);
        List chartResult = new ArrayList();
        if (type.equals("my")){
            List<Chart> chartList = chartMapper.getChartByCreat(name,1,title);
//            chartList.sort(Comparator.comparing(Chart::getCreateTime).reversed());
            // List sche = new ArrayList();
            for (Chart ch:chartList){
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                /* if (null != qd) {
                  List<DashChart> dachList = dashChartMapper.selectByChartId(ch.getId(),name,1);
                  for (DashChart dach:dachList){
                    Integer dashsche = dashboardMapper.selectDashById(dach.getDashboardId(),name,1);
                    sche.add(dashsche);
                  }
                  if (sche.contains(1)){
                    LocalDate dt = LocalDate.now();
                    String url = gatewayUrl+"/taskinstance/page?taskId="+qd.getDetaskid()+"&name="+qd.getDb()+"&pageNum=1&pageSize=1&start_date="+dt+"+00:00:00&end_date="+dt+"+23:59:59";
                    Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseTask = client.newCall(requestTask).execute();
                    System.out.println("拿状态");
                    String responseTaskBody = responseTask.body().string();
                    JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                    JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                    JSONObject shareidListResult = shareidListData.getJSONObject("result");
                    JSONArray shareidList = shareidListResult.getJSONArray("list");
                    if (shareidList.size()!=0){
                        JSONObject status = (JSONObject)shareidList.get(0);
                        chartMapper.updateStatus(ch.getId(),status.get("state").toString());
                    }else {
                        chartMapper.updateStatus(ch.getId(),"waitting");
                    }
                  }
                  else {
                    if (!ch.getStatus().equals("success")){
                        LocalDate dt = LocalDate.now();
                        String url = gatewayUrl+"/taskinstance/page?taskId="+qd.getDetaskid()+"&name="+qd.getDb()+"&pageNum=1&pageSize=1";
                        Request requestTask = new Request.Builder()
                                .url(url)
                                .method("GET", null)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseTask = client.newCall(requestTask).execute();
                        System.out.println("拿状态");
                        String responseTaskBody = responseTask.body().string();
                        JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                        JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                        JSONObject shareidListResult = shareidListData.getJSONObject("result");
                        JSONArray shareidList = shareidListResult.getJSONArray("list");
                        JSONObject status = (JSONObject)shareidList.get(0);
                        chartMapper.updateStatus(ch.getId(),status.get("state").toString());
                    }
                  }
                } */
                QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                String uuidTime = new String();
                if (qd == null){
                    uuidTime = "null";
                }else{
                    uuidTime = date.format(qd.getCreateTime().getTime());
                }
                Map chMap = new HashMap();
                chMap.put("content",ch.getContent());
                chMap.put("createBy",ch.getCreateBy());
                chMap.put("createTime",ch.getCreateTime());
                chMap.put("describeChart",ch.getDescribeChart());
                chMap.put("id",ch.getId());
                chMap.put("isActive",ch.getIsActive());
                chMap.put("isFavorate",ch.getIsFavorate());
                chMap.put("isShare",ch.getIsShare());
                chMap.put("name",ch.getName());
                chMap.put("param",ch.getParam());
                chMap.put("querySql",ch.getQuerySql());
                chMap.put("type",ch.getType());
                chMap.put("updateBy",ch.getUpdateBy());
                chMap.put("updateTime",ch.getUpdateTime());
                chMap.put("uuid",ch.getUuid());
                chMap.put("uuid_time",uuidTime);
                chMap.put("columnList",qh.getColumnType());
                chMap.put("status",ch.getStatus());
                chartResult.add(chMap);
            }
        }
        if (type.equals("tuck")){
            List<Integer> favorChart = favorDashChartMapper.getFavorChart(name, "chart", 1);
            for (Integer id:favorChart){
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsFavorate(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chartResult.add(chMap);
                }
            }
        }
        if (type.equals("share")){
            List<Integer> shareChart = shareMapper.getShareChart(name,"chart");
            for (Integer id:shareChart){
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsShare(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chartResult.add(chMap);
                }
            }
        }
        if (type.equals("all")){
            List<Chart> chartList = chartMapper.getChartByCreat(name,1,title);
            List<Integer> shareChart = shareMapper.getShareChart(name,"chart");
            for (Chart ch:chartList){
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                String uuidTime = new String();
                if (qd == null){
                    uuidTime = "null";
                }else{
                    uuidTime = date.format(qd.getCreateTime().getTime());
                }
                Map chMap = new HashMap();
                chMap.put("content",ch.getContent());
                chMap.put("createBy",ch.getCreateBy());
                chMap.put("createTime",ch.getCreateTime());
                chMap.put("describeChart",ch.getDescribeChart());
                chMap.put("id",ch.getId());
                chMap.put("isActive",ch.getIsActive());
                chMap.put("isFavorate",ch.getIsFavorate());
                chMap.put("isShare",ch.getIsShare());
                chMap.put("name",ch.getName());
                chMap.put("param",ch.getParam());
                chMap.put("querySql",ch.getQuerySql());
                chMap.put("type",ch.getType());
                chMap.put("updateBy",ch.getUpdateBy());
                chMap.put("updateTime",ch.getUpdateTime());
                chMap.put("uuid",ch.getUuid());
                chMap.put("uuid_time",uuidTime);
                chMap.put("columnList",qh.getColumnType());
                chartResult.add(chMap);
            }
            for (Integer id:shareChart){
                //todo：is_share置1给前端做一个校验
                Chart ch = chartMapper.getChartForView(id,1);
                if (ch != null){
                    ch.setIsShare(1);
                    QueryData qd = queryDataMapper.getQd(ch.getUuid());
                    QueryHistory qh = queryHistoryMapper.selectByUuid(ch.getUuid());
                    SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
                    String uuidTime = new String();
                    if (qd == null){
                        uuidTime = "null";
                    }else{
                        uuidTime = date.format(qd.getCreateTime().getTime());
                    }
                    Map chMap = new HashMap();
                    chMap.put("content",ch.getContent());
                    chMap.put("createBy",ch.getCreateBy());
                    chMap.put("createTime",ch.getCreateTime());
                    chMap.put("describeChart",ch.getDescribeChart());
                    chMap.put("id",ch.getId());
                    chMap.put("isActive",ch.getIsActive());
                    chMap.put("isFavorate",ch.getIsFavorate());
                    chMap.put("isShare",ch.getIsShare());
                    chMap.put("name",ch.getName());
                    chMap.put("param",ch.getParam());
                    chMap.put("querySql",ch.getQuerySql());
                    chMap.put("type",ch.getType());
                    chMap.put("updateBy",ch.getUpdateBy());
                    chMap.put("updateTime",ch.getUpdateTime());
                    chMap.put("uuid",ch.getUuid());
                    chMap.put("uuid_time",uuidTime);
                    chMap.put("columnList",qh.getColumnType());
                    chartResult.add(chMap);
                }
            }
        }
        PageInfo pageInfo = new PageInfo<>(chartResult);
        pageInfo.setTotal(t);
        return pageInfo;
    }

    @Override
    public void updateChartStatus(String type, String name, String title, String token) {
        try {
            if (!type.equals("my")) {
             return;
            }
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            List<Chart> chartList = chartMapper.getChartByCreat(name,1,title);
            List sche = new ArrayList();
            for (Chart ch:chartList) {
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                if (null == qd) {
                    log.info(String.format("%s update chart status %d query data is null",
                            name, ch.getId()));
                    continue;
                }
                List<DashChart> dachList = dashChartMapper.selectByChartId(ch.getId(),name,1);
                for (DashChart dach:dachList){
                    Integer dashsche = dashboardMapper.selectDashById(dach.getDashboardId(),name,1);
                    sche.add(dashsche);
                }
                if (sche.contains(1)){
                    LocalDate dt = LocalDate.now();
                    String url = gatewayUrl+"/taskinstance/page?taskId="+qd.getDetaskid()+"&name="+qd.getDb()+"&pageNum=1&pageSize=1&start_date="+dt+"+00:00:00&end_date="+dt+"+23:59:59";
                    Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseTask = client.newCall(requestTask).execute();
                    System.out.println("拿状态");
                    String responseTaskBody = responseTask.body().string();
                    log.info(String.format("%s update chart status %d [%s] get %s",
                              name, ch.getId(), url, responseTaskBody));
                    JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                    JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                    JSONObject shareidListResult = shareidListData.getJSONObject("result");
                    JSONArray shareidList = shareidListResult.getJSONArray("list");
                    if (shareidList.size()!=0){
                        JSONObject status = (JSONObject)shareidList.get(0);
                        chartMapper.updateStatus(ch.getId(),status.get("state").toString());
                        log.info(String.format("%s update chart status %d to %s",
                                name, ch.getId(), status.get("state").toString()));
                    }else {
                        chartMapper.updateStatus(ch.getId(),"waitting");
                        log.info(String.format("%s update chart status %d to waitting",
                                name, ch.getId()));
                    }
                } else {
                    if (!ch.getStatus().equals("success")){
                        LocalDate dt = LocalDate.now();
                        String url = gatewayUrl+"/taskinstance/page?taskId="+qd.getDetaskid()+"&name="+qd.getDb()+"&pageNum=1&pageSize=1";
                        Request requestTask = new Request.Builder()
                                .url(url)
                                .method("GET", null)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseTask = client.newCall(requestTask).execute();
                        System.out.println("拿状态");
                        String responseTaskBody = responseTask.body().string();
                        log.info(String.format("%s update chart status %d [%s] get %s",
                                name, ch.getId(), url, responseTaskBody));
                        JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                        JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                        JSONObject shareidListResult = shareidListData.getJSONObject("result");
                        JSONArray shareidList = shareidListResult.getJSONArray("list");
                        JSONObject status = (JSONObject)shareidList.get(0);
                        chartMapper.updateStatus(ch.getId(),status.get("state").toString());
                        log.info(String.format("%s update chart status %d to %s",
                                name, ch.getId(), status.get("state").toString()));
                    }
                }
            }
        } catch (Exception e){
            log.error(String.format("There is a stack err when %s update chart status %s",
                    name, CommonUtil.printStackTraceToString(e)));
        }
    }

    @Override
    public List<Map<String,String>> getJinja(String shareid, List<String> jinja){
        try {
            String jinjaUrl = gatewayUrl.replaceAll("/+[^/]*$", "") + "/pipeline/render";
            Map addTaskValues = new HashMap<>();
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            String values = new String();
            for (int i =0 ;i<jinja.size();i++){
                String newDt = "{{" + jinja.get(i) +"}}";
                values += newDt;
                values += ",";
            }
            String str = values.substring(0,values.length()-1);
            addTaskValues.put("execution_date",LocalDateTime.now().toString());
            addTaskValues.put("content",str);
            String valuesForAddTask = JSONObject.toJSONString(addTaskValues);
            log.info(String.format("%s jinja send %s",
                    shareid, valuesForAddTask));
            RequestBody bodyForAddTask = RequestBody.create(mediaType, valuesForAddTask);
            Request requestAddTask = new Request.Builder()
                    .url(jinjaUrl)
                    .method("POST", bodyForAddTask)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response responseAddTask = client.newCall(requestAddTask).execute();
            String responseAddTaskBody = responseAddTask.body().string();
            log.info(String.format("%s jinja get %s",
                    shareid, responseAddTaskBody));
            JSONObject jsonAddTaskObj = JSONObject.parseObject(responseAddTaskBody);
            Integer data12 = jsonAddTaskObj.getInteger("code");
            String getdata = jsonAddTaskObj.getString("data");
//            JSONObject getdata = jsonAddTaskObj.getJSONObject("data");
            String[] data = JSONObject.toJSONString(getdata).split(",");
            List<String> timeData = new ArrayList<>();
            if (data.length == 1){
                String time = data[0];
                time = data[0].substring(1,data[0].length());
                String timenew = time.substring(0,time.length()-1);
                timeData.add(timenew);
            }else {
                for (int i =0 ;i<data.length;i++){
                    String time = data[i];
                    if (i==0){
                        time = data[i].substring(1,data[0].length());
                    }
                    if (i==data.length-1){
                        time = data[i].substring(0,data[i].length()-1);
                    }
                    timeData.add(time);
                }
            }
            List<Map<String,String>> result = new ArrayList();
            Map<String,String> dtData = new HashMap();
            for (int i =0 ;i<jinja.size();i++){
                dtData.put(jinja.get(i),timeData.get(i));
                result.add(dtData);
            }
            return result;
        }catch (Exception e){
            throw new ServiceException(BaseResponseCodeEnum.valueOf(e.toString()));
        }
    }

    @Override
    @Cacheable(cacheNames = {"bi"}, key = "#tenantName+'-'+#name+'-'+#chid+'-'+#bidate", unless = "#result == null||#result.size() > 100000")
    public List<ResultSet> getDataback(String name, String tenantName, Map<String, Object> params, String bidate, Integer chid) throws SQLException {
        if (params.containsKey("ds-user")) {
            params.remove("ds-user");
        }
        log.info("params————————————");
        log.info(params.toString());
        Set keySet = params.keySet();
        log.info("前端传值的keyset个数——————————");
        log.info(keySet.toString());
        String driver="com.mysql.jdbc.Driver";
        Integer cid = Integer.parseInt((String) params.get("id"));
        String bidt = (String) params.get("bidt");
        String uuid = chartMapper.getUuid(cid,1);
        QueryData qd = queryDataMapper.getQd(uuid);
        String region = qd.getRegion();
        String db = qd.getDb();
        List resultdata = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();
        //只有bidt和id
        if (keySet.size() == 2){
            String db_name = "query_bi";
            if (!tenantName.equals("admin")) {
                db_name += "_" + tenantName;
            }
            String sql = "select * from " + db_name + "." + db +" where bidt=?";
            String dropIndex = "DROP INDEX bidt on " + db_name + "." + db +";";
            String index = "ALTER TABLE " + db_name + "." + db+" ADD INDEX (bidt);";
            if (region.equals("aws_ue1")){
                Connection con = DriverManager.getConnection(cloudawsUrl, cloudawsUsername, cloudawsPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                pStemt.setString(1, bidt);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
                log.info("columnNames都有——————————");
                log.info(columnNames.toString());
                log.info("columnTypes都有——————————");
                log.info(columnTypes.toString());
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
//                log.info(resultSet.toString());
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                //一条一条数据取
                while (resultSet.next()){
                    log.info("开始获取数据——————————");
                    Map resultMap = new HashMap();
                    List result = new ArrayList<>();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));

                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else if (region.equals("aws_sg")){
                Connection con = DriverManager.getConnection(cloudawsSGUrl, cloudawsSGUsername, cloudawsSGPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                pStemt.setString(1, bidt);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));

                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else if (region.equals("huawei_sg")){
                Connection con = DriverManager.getConnection(cloudhuaweiUrl, cloudhuaweiUsername, cloudhuaweiPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                pStemt.setString(1, bidt);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else {
                Connection con = DriverManager.getConnection(
                        biConfig.getUrl().get(region),
                        biConfig.getUsername().get(region),
                        biConfig.getPassword().get(region));
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            }
        }
        if (keySet.size()>2){
            String db_name = "query_bi";
            if (!tenantName.equals("admin")) {
                db_name += "_" + tenantName;
            }
            String dropIndex = "DROP INDEX bidt on " + db_name + "." + db +";";
            String index = "ALTER TABLE " + db_name + "." + db+" ADD INDEX (bidt);";
            String wh = "where";
            String whcolumn = new String();
            for (Object key:keySet){
                String columnm = (String) key;
                if (!columnm .equals("ds-user")) {
                    whcolumn =whcolumn + " "+ columnm + " like %" + params.get(key) + "% and";
                }
            }
            wh  = wh + whcolumn + " bidt=" + bidt;
            String sql = "select * from " + db_name + "." + db ;
            sql += wh;
            if (region.equals("aws_ue1")){
                Connection con = DriverManager.getConnection(cloudawsUrl, cloudawsUsername, cloudawsPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));

                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else if (region.equals("aws_sg")){
                Connection con = DriverManager.getConnection(cloudawsSGUrl, cloudawsSGUsername, cloudawsSGPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));

                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else if (region.equals("huawei_sg")){
                Connection con = DriverManager.getConnection(cloudhuaweiUrl, cloudhuaweiUsername, cloudhuaweiPassword);
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            } else {
                Connection con = DriverManager.getConnection(
                        biConfig.getUrl().get(region),
                        biConfig.getUsername().get(region),
                        biConfig.getPassword().get(region));
                Statement statement = con.createStatement();
                PreparedStatement pStemtIndex = con.prepareStatement(index);
                pStemtIndex.executeUpdate();
                PreparedStatement pStemt = con.prepareStatement(sql);
                //结果集元数据
                ResultSetMetaData rsmd = pStemt.getMetaData();
                //表列数
                int size = rsmd.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnNames.add(rsmd.getColumnName(i + 1));
                    columnTypes.add(rsmd.getColumnTypeName(i + 1));
                }
//                resultdata.add(columnNames);
//                resultdata.add(columnTypes);
                ResultSet resultSet = pStemt.executeQuery();
                log.info("获取数据：result获取为——————————————");
                log.info(resultSet.toString());
                //一条一条数据取
                while (resultSet.next()){
                    List result = new ArrayList<>();
                    Map resultMap = new HashMap();
                    for (int j = 0;j<columnNames.size();j++){
                        log.info("开始往返回列表里写入数据——————————");
                        if (columnTypes.get(j).equals("VARCHAR")){
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("INT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("FLOAT")){
//                            result.add(resultSet.getFloat(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getFloat(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DOUBLE")){
//                            result.add(resultSet.getDouble(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDouble(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("BIGINT")){
//                            result.add(resultSet.getInt(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getInt(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATETIME")){
//                            result.add(resultSet.getTime(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getTime(columnNames.get(j)));
                        }
                        else if (columnTypes.get(j).equals("DATE")){
//                            result.add(resultSet.getDate(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getDate(columnNames.get(j)));
                        }
                        else {
//                            result.add(resultSet.getString(columnNames.get(j)));
                            resultMap.put(new String(columnNames.get(j)),resultSet.getString(columnNames.get(j)));
                        }
                    }
                    resultdata.add(resultMap);
                    log.info("获取数据：resultdata返回前端结果为——————————————");
//                    log.info(resultdata.toString());
                }
                PreparedStatement pStemtDropIndex = con.prepareStatement(dropIndex);
                pStemtDropIndex.executeUpdate();
                con.close();
            }
        }
        return resultdata;
    }

    @Override
    public void test(Integer id) throws IOException {
        String testtoken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3NjFkMmMwOWI4MGU0N2FmYWE3NGE0MDBkNTRjNTYzYiIsImlhdCI6MTY3MDIxMDE2OCwic3ViIjoidG9rZW4gYnkgdXNoYXJlaXQiLCJleHAiOjE2NzAyOTY1Njh9.br-CQMCToUVkbBn_XeW2QMrTxl5rjlpLHpmSH3RcYKE";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        LocalDate timeDate = LocalDate.now();
        String backfill = gatewayUrl + "/task/backfill?id=";
        backfill = backfill + id + "&startDate=" + timeDate + "+00:00:00" + "&endDate=" + timeDate + "+23:59:59&childIds=&isCheckDependency=true&isSendNotify=true";
        Request requestEditTask = new Request.Builder()
                .url(backfill)
                .method("GET", null)
                .addHeader("Authentication", testtoken)
                .addHeader("Content-Type", "application/json")
                .build();
        Response responseEditTask = client.newCall(requestEditTask).execute();
        String responseEditTaskBody = responseEditTask.body().string();
        JSONObject jsonEditTaskObj = JSONObject.parseObject(responseEditTaskBody);
        if (!jsonEditTaskObj.get("code").equals(0)){
            throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("补数失败，调度id： %s",id)));
        }
    }

    @Override
    public void addViewLog(String name, Integer id){
        LocalDateTime time = LocalDateTime.now();
        logViewMapper.addLog(name, id, time, "chart");
    }

    @Override
    public void online(String shareid, Integer id) throws IOException{
        String testtoken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4Y2E2MTQ3Y2I0NDg0YjJiYTMzODJmMDIyNzJhOTA1OCIsImlhdCI6MTY3MDgxMTc0OSwic3ViIjoidG9rZW4gYnkgdXNoYXJlaXQiLCJleHAiOjE2NzA4OTgxNDl9.6OY9E_Rj-MdyxcHDJigqnMn9TWyXp5wEH4EqLp2ovXU";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        String onlineAndOffline = gatewayUrl + "/task/start?id=";
        onlineAndOffline += id;
        RequestBody body = RequestBody.create(mediaType, "");
        Request requestOnlineTask = new Request.Builder()
                .url(onlineAndOffline)
                .method("PATCH",body)
                .addHeader("Authentication", testtoken)
                .addHeader("Content-Type", "application/json")
                .build();
        Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
        String responseOnlineTaskBody = responseOnlineTask.body().string();
        log.info(String.format("%s online DE [%s] get %s",
                shareid, onlineAndOffline, responseOnlineTaskBody));
        JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
        if (!jsonOnlineTaskObj.get("code").equals(0)){
            throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",id)));
        }
    }

    public String encrypt(String data) throws Exception {
        //创建秘钥
        String key = "DataStudio-20210628";
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
        //加密
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] result =  cipher.doFinal(data.getBytes());
        //使用base64进行编码
        return Base64.getEncoder().encodeToString(result);
    }

    @Override
    @Async
    public void addDE(String shareid, HashMap<String,String> map, String token,String name, String tenantName)
            throws Exception {
        InfTraceContextHolder.get().setTenantName(tenantName);
        List<Map> partitions = new ArrayList<>();
        String chartSql = map.get("querySql");
        String addUrl = gatewayUrl+"/task/add";
        JSONArray columnList = JSONArray.parseArray(map.get("columnList").toString());
        LocalDateTime time = LocalDateTime.now();
        String paramUuid = map.get("uuid");
        List<QueryData> uuidForDe = queryDataMapper.selectUuidForDe(paramUuid,1);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        //获取当前用户的所有用户组
        System.out.println("开始前时间");
        System.out.println(LocalDateTime.now());
        String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
        Request requestGetGroup = new Request.Builder()
                .url(getGroupUrl)
                .method("GET", null)
                .addHeader("Authentication", token)
                .addHeader("Content-Type", "application/json")
                .build();
        Response responseGetGroup = client.newCall(requestGetGroup).execute();
        String responseGetGroupBody = responseGetGroup.body().string();
        log.info(String.format("%s add chart get group info get %s",
                shareid, responseGetGroupBody));
        JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
        JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
        Integer groupId = (Integer) data.get("id");
        List<Map> costList = new ArrayList<>();
        Map cost = new HashMap();
        cost.put("key", groupId);
        cost.put("value", 100);
        costList.add(cost);
        log.info("添加chart：用户组别信息————————————————");
        log.info(cost.toString());
        if (uuidForDe.size() == 0) {
            //新增一个调度
            Map addTaskValues = new HashMap<>();
            String uuidName = paramUuid.replace("-","_");
            String taskDataName = "bi_"+uuidName+"_dataset_"+shareid;
            Map runtimeConfig = new HashMap();
            List columns = new ArrayList();
            Map partitionsMap = new HashMap();
            partitionsMap.put("name", "bidt");
            partitionsMap.put("value", "{{ ds_nodash }}");
            partitions.add(partitionsMap);

            Map crontabParam = new HashMap();
            crontabParam.put("endTime", "23:59");
            crontabParam.put("fixedTime", "00:00");
            crontabParam.put("interval", 5);
            crontabParam.put("range", new ArrayList<>());
            crontabParam.put("startTime", "00:00");

            Map triggerParam = new HashMap();
            triggerParam.put("crontab", "00 00 * * *");
            triggerParam.put("outputGranularity", "daily");
            triggerParam.put("type", "cron");
            triggerParam.put("crontabParam", crontabParam);
//            String encodeSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartSql,"UTF-8").getBytes("utf-8")));
            //todo:加调度参数
            Map typeMap = new HashMap();
            typeMap.put("string","varchar(255)");
            typeMap.put("int","int");
            typeMap.put("double","double");
            typeMap.put("tinyint","int");
            typeMap.put("smallint","int");
            typeMap.put("bigint","bigint");
            typeMap.put("float","float");
            typeMap.put("","varchar(255)");
            typeMap.put("integer","int");
            for (int i = 0; i < columnList.size(); i++) {
                Map colmap = (Map) columnList.get(i);
                for (Object key : colmap.keySet()) {
                    Map col = new IdentityHashMap();
                    col.put(new String("columnName"), key.toString());
                    col.put(new String("columnType"), typeMap.get(colmap.get(key.toString()).toString().trim()));
                    columns.add(col);
                }
            }
            log.info("添加chart：传给DE的表字段信息——————————");
            log.info(columns.toString());
            runtimeConfig.put("acrossCloud", "common");
            runtimeConfig.put("alertMethod", new ArrayList<>());
            runtimeConfig.put("alertType", new ArrayList<>());
            runtimeConfig.put("batchParams", "");
            runtimeConfig.put("clusterSla", "normal");
            runtimeConfig.put("collaborators", new ArrayList<>());
            runtimeConfig.put("columns", columns);
            {
                runtimeConfig.put("connectionUrl", biConfig.getUrl().get(map.get("region")));
                runtimeConfig.put("dbUser", biConfig.getUsername().get(map.get("region")));
                String password = encrypt(biConfig.getPassword().get(map.get("region")));
                runtimeConfig.put("dbPassword", password);
            }
            if (map.get("region").equals("aws_ue1")) {
                runtimeConfig.put("connectionUrl", cloudawsUrl);
                runtimeConfig.put("dbUser", cloudawsUsername);
                String password = encrypt(cloudawsPassword);
                runtimeConfig.put("dbPassword", password);
            }
            if (map.get("region").equals("aws_sg")) {
                runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                runtimeConfig.put("dbUser", cloudawsSGUsername);
                String password = encrypt(cloudawsSGPassword);
                runtimeConfig.put("dbPassword", password);
            }
            if (map.get("region").equals("huawei_sg")) {
                runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                runtimeConfig.put("dbUser", cloudhuaweiUsername);
                String password = encrypt(cloudhuaweiPassword);
                runtimeConfig.put("dbPassword", password);
            }
            runtimeConfig.put("dsGroups", new ArrayList<>());
            runtimeConfig.put("emails", "");
            runtimeConfig.put("endDate", "2010-11-10 23:59:59");
//            runtimeConfig.put("endDate", "");
            runtimeConfig.put("executionTimeout", 0);
            runtimeConfig.put("existTargetTable", false);
            runtimeConfig.put("group", "");
            runtimeConfig.put("lifecycle", "Ec2spot");
            runtimeConfig.put("maxActiveRuns", 1);
            runtimeConfig.put("owner", name);
            runtimeConfig.put("resourceLevel", "standard");
            runtimeConfig.put("partitions", partitions);
            runtimeConfig.put("retries", 1);
            runtimeConfig.put("source", "task");
            runtimeConfig.put("sourceColumns", new ArrayList<>());
            if (map.get("region").equals("aws_ue1")) {
                runtimeConfig.put("sourceRegion", "ue1");
            }
            if (map.get("region").equals("aws_sg")) {
                runtimeConfig.put("sourceRegion", "sg1");
            }
            if (map.get("region").equals("huawei_sg")) {
                runtimeConfig.put("sourceRegion", "sg2");
            }
            runtimeConfig.put("startDate", "2010-11-10 00:00:00");
//            runtimeConfig.put("startDate", "");
            if (tenantName.equals("admin")){
                runtimeConfig.put("targetDB", "query_bi");
            }else {
                String dbName = "query_bi";
                runtimeConfig.put("targetDB",dbName + "_" + tenantName);
            }
            runtimeConfig.put("targetTable", taskDataName);
            runtimeConfig.put("cost",costList);

            addTaskValues.put("eventDepends", new ArrayList<>());
            addTaskValues.put("dependTypes", new ArrayList<>());
            addTaskValues.put("description", "");
            addTaskValues.put("content",map.get("content"));
            String engine = map.get("engine");
            if (null != engine && engine.startsWith("presto")) {
                addTaskValues.put("templateCode", "TrinoJob");
            } else {
                addTaskValues.put("templateCode", "Hive2Mysql");
            }
            addTaskValues.put("inputDataset", new ArrayList<>());
            addTaskValues.put("outputDataset", new ArrayList<>());
            addTaskValues.put("invokingStatus", true);
            addTaskValues.put("name", taskDataName);
            addTaskValues.put("runtimeConfig", runtimeConfig);
            addTaskValues.put("triggerParam", triggerParam);

            String valuesForAddTask = JSONObject.toJSONString(addTaskValues);
            log.info(String.format("%s add chart add DE send %s",
                            shareid, valuesForAddTask));
            RequestBody bodyForAddTask = RequestBody.create(mediaType, valuesForAddTask);
            Request requestAddTask = new Request.Builder()
                    .url(addUrl)
                    .method("POST", bodyForAddTask)
                    .addHeader("Authentication", token)
                    .addHeader("Content-Type", "application/json;charset=utf-8")
                    .build();
            Response responseAddTask = client.newCall(requestAddTask).execute();
            String responseAddTaskBody = responseAddTask.body().string();
            log.info("添加chart：创建DE任务结果——————————");
            log.info(String.format("%s add chart add DE get %s",
                            shareid, responseAddTaskBody));
            JSONObject jsonAddTaskObj = JSONObject.parseObject(responseAddTaskBody);
            Integer taskId = (Integer) jsonAddTaskObj.getJSONObject("data").get("id");
            //todo:把taskid加到query_data那个表里
            boolean addTaskIdToQueryData = queryDataMapper.addTaskIdToQueryData(taskId, paramUuid, map.get("region").toString(), taskDataName, time, 1);
            if (!jsonAddTaskObj.get("code").equals(0)) {
//                throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("新增调度失败，调度id： %s",taskId)));
                throw new ServiceException(BaseResponseCodeEnum.SCHEDULER_RESPONSE_ERR);

            }
            TimeUnit.SECONDS.sleep(1);
            //todo:上线
            String onlineAndOffline = gatewayUrl+"/task/start?id=";
            onlineAndOffline += taskId;
            RequestBody body = RequestBody.create(mediaType, "");
            Request requestOnlineTask = new Request.Builder()
                    .url(onlineAndOffline)
                    .method("PATCH",body)
                    .addHeader("Authentication", token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
            String responseOnlineTaskBody = responseOnlineTask.body().string();
            log.info("添加chart：DE任务上线结果——————————");
            log.info(String.format("%s add chart start DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
            JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
            if (!jsonOnlineTaskObj.get("code").equals(0)){
//                throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",taskId)));
                throw new ServiceException(BaseResponseCodeEnum.SCHEDULER_RESPONSE_ERR);

            }
            TimeUnit.SECONDS.sleep(1);
            //todo：更新当日补数
            LocalDate timeDate = LocalDate.now();
            String backfill = gatewayUrl+"/task/backfill?id=";
            backfill = backfill + taskId + "&startDate=" + timeDate + "+00:00:00" + "&endDate=" + timeDate + "+23:59:59&childIds=&isCheckDependency=true&isSendNotify=true";
            Request requestEditTask = new Request.Builder()
                    .url(backfill)
                    .method("GET", null)
                    .addHeader("Authentication", token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response responseEditTask = client.newCall(requestEditTask).execute();
            String responseEditTaskBody = responseEditTask.body().string();
            log.info("添加chart：DE任务补数结果——————————");
            log.info(String.format("%s add chart backfill DE [%s] get %s",
                            shareid, backfill, responseEditTaskBody));
            JSONObject jsonEditTaskObj = JSONObject.parseObject(responseEditTaskBody);
            System.out.println("结束后时间");
            System.out.println(LocalDateTime.now());
//            if (!jsonEditTaskObj.get("code").equals(0)){
//                return (String.format("补数失败，调度id： %s",taskId));
//            }
        }
    }
}
