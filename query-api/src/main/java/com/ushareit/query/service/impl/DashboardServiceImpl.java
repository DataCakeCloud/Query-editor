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
import com.ushareit.query.service.DashboardService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.web.utils.CommonUtil;
import io.prestosql.jdbc.$internal.okhttp3.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
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
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;


/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Slf4j
@Service
@Setter
public class DashboardServiceImpl extends AbstractBaseServiceImpl<Dashboard> implements DashboardService {
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
    private DashboardMapper dashboardMapper;

    @Resource
    private SharebiMapper shareMapper;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private QueryDataMapper queryDataMapper;

    @Resource
    private DashChartMapper dashChartMapper;

    @Resource
    private ClassificationDashMapper classificationDashMapper;

    @Resource
    private FavorDashChartMapper favorDashChartMapper;

    @Resource
    private LogViewMapper logViewMapper;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Override
    public CrudMapper<Dashboard> getBaseMapper() { return dashboardMapper; }

    @Override
    public int addShareGrade(Sharebi sh, String shareeEmail) {
        try {
            Sharebi share = shareMapper.selectByShare(sh.getSharer(),sh.getSharee(),sh.getType(),sh.getShareId());
            if (share==null){
                shareMapper.addNewShare(sh);
            }
            int chartID = sh.getShareId();

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
            String link = sh.getShareUrl();
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

    @Override
    public String updateDash(HashMap<String, String > map, String shareid,String token,String name,String tenantName){
        try {
            log.info("更新看板：前端传来的contab——————————");
            log.info(map.get("crontab"));
            Dashboard dash = new Dashboard();
            dash.setDescribeDash(map.get("describe"));
            dash.setIsActive(1);
            dash.setClassId(Integer.parseInt(map.get("classid")));
            dash.setParam(map.get("params"));
            dash.setIsSchedule(Integer.parseInt(map.get("is_schedule")));
            dash.setName(map.get("name"));
            dash.setIsShare(0);
            dash.setCrontab(map.get("crontab"));
            Integer id = Integer.parseInt(map.get("id"));
            List<Object> list = JSONArray.parseArray(JSON.toJSONString(map.get("chart_id")));
            List<Integer> chartList = new ArrayList<>();
            for (Object object : list){
                Integer chartId = Integer.valueOf(object.toString());
                chartList.add(chartId);
            }
            List<Integer> cidList = dashChartMapper.selectByIdDashId(id,1);
            for (Integer chartId:cidList){
                if (!chartList.contains(chartId)){
                    dashChartMapper.updateActive(id,chartId,0);
                }
            }
            Dashboard dashboard = dashboardMapper.selectById(id, 1);
            //更新看板chart表
            for (Integer chid:chartList){
                DashChart dashchart = dashChartMapper.selectByIdChartId(id,chid,1);
                if (dashchart == null){
                    DashChart dc = new DashChart();
                    dc.setDashboardId(id);
                    dc.setChartId(chid);
                    dc.setIsActive(1);
                    dc.setCreateBy(name);
                    dc.setUpdateBy(name);
                    dc.setCreateTime(new Timestamp(System.currentTimeMillis()));
                    dc.setUpdateTime(new Timestamp(System.currentTimeMillis()));
                    dashChartMapper.insert(dc);
                }
            }
            //更新看板信息
            if (dashboard != null){
                LocalDateTime upTime = LocalDateTime.now();
                dashboardMapper.updateByDash(dash,upTime,id);
            } else {
                throw new ServiceException(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL);
            }
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s update dash %s",
            		shareid, CommonUtil.printStackTraceToString(e)));
            throw e;
        }
        return "success";
    }

    @Override
    @Async
    public void updateDashDE(HashMap<String, String > map, String shareid,String token,String name,String tenantName){
        try {
            InfTraceContextHolder.get().setTenantName(tenantName);
            log.info("更新看板：前端传来的contab——————————");
            log.info(map.get("crontab"));
            Integer id = Integer.parseInt(map.get("id"));
            List<Object> list = JSONArray.parseArray(JSON.toJSONString(map.get("chart_id")));
            List<Integer> chartList = new ArrayList<>();
            for (Object object : list){
                Integer chartId = Integer.valueOf(object.toString());
                chartList.add(chartId);
            }
            Dashboard dashboard = dashboardMapper.selectById(id, 1);
            Integer sche = dashboard.getIsSchedule();
            Integer isSche = Integer.parseInt(map.get("is_schedule"));
            log.info("更新看板：看板原始调度——————————");
            log.info(sche.toString());
            log.info("更新看板：看板更新调度——————————");
            log.info(isSche.toString());
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            //依照dashboard调度情况更新chart调度
            if (sche.equals(0) && isSche.equals(1)){
                //todo:
                // 1、拿chartlist的id找每一个chart的uuid；
                // 2、拿到uuid再找调度id；3、调度id拿到后直接调更改接口把时间更改成""
                for (Integer chid:chartList){
                    String uuid = chartMapper.getUuid(chid,1);
                    Integer deTaskId = queryDataMapper.selectDeTaskId(uuid,1);
                    //todo：调接口把这个deTaskId的调度开启天粒度
                    String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
                    Request requestGetGroup = new Request.Builder()
                            .url(getGroupUrl)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseGetGroup = client.newCall(requestGetGroup).execute();
                    String responseGetGroupBody = responseGetGroup.body().string();
                    log.info(String.format("%s update dashboard get group info get %s",
                            shareid, responseGetGroupBody));
                    JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
                    JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
                    Integer groupId = (Integer) data.get("id");
                    List<Map<String,Object>> costList = new ArrayList<>();
                    Map<String,Object> cost = new HashMap<>();
                    cost.put("key", groupId);
                    cost.put("value", 100);
                    costList.add(cost);
                    Chart chartDetail = chartMapper.getChartForView(chid,1);
                    Integer taskId = queryDataMapper.selectDeTaskId(uuid,1);
                    QueryData qd = queryDataMapper.getQd(uuid);
                    QueryHistory qh = queryHistoryMapper.selectByUuid(uuid);
                    JSONArray columnList = JSONArray.parseArray(qh.getColumnType());
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
                    log.info("更新看板：0->1 先下线任务——————————");
                    log.info(String.format("%s update dashborad offline DE [%s] get %s",
                            shareid, offLine, responseOffLineTaskBody));
                    TimeUnit.SECONDS.sleep(1);
                    //todo:更新crontab时间以及高级设置
                    String updateUrl = gatewayUrl+"/task/updateAndStart";
                    List<Map<String,Object>> partitions = new ArrayList<>();
                    Map<String,Object> addTaskValues = new HashMap<>();
                    String taskName = "bi_" + chid + "_dataset_" + shareid;
                    String uuidName = uuid.replace("-","_");
                    String taskDataName = "bi_"+uuidName+"_dataset_"+shareid;
                    log.info("更新看板：DE的任务名称以及导入mysql表名称");
                    log.info(uuidName);
                    log.info(taskDataName);
                    Map<String,Object> runtimeConfig = new HashMap<>();
                    List<Map<String,Object>> columns = new ArrayList<>();
                    Map<String,Object> partitionsMap = new HashMap<>();
                    partitionsMap.put("name", "bidt");
                    partitionsMap.put("value", "{{ ds_nodash }}");
                    partitions.add(partitionsMap);

                    Map<String,Object> crontabParam = new HashMap<>();
                    crontabParam.put("endTime", "23:59");
                    crontabParam.put("fixedTime", "00:00");
                    crontabParam.put("interval", 5);
                    crontabParam.put("range", new ArrayList<>());
                    crontabParam.put("startTime", "00:00");

                    Map<String,Object> triggerParam = new HashMap<>();
                    triggerParam.put("crontab", map.get("crontab"));
                    triggerParam.put("outputGranularity", "daily");
                    triggerParam.put("type", "cron");
                    triggerParam.put("crontabParam", crontabParam);
                    String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartDetail.getQuerySql()).getBytes()));
                    //todo:加调度参数
                    Map<String,Object> typeUpMap = new HashMap<>();
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
                        Map<String,Object> colmap = (Map) columnList.get(i);
                        for (Object key : colmap.keySet()) {
                            Map<String,Object> col = new IdentityHashMap<>();
                            col.put(new String("columnName"), key.toString());
                            col.put(new String("columnType"), typeUpMap.get(colmap.get(key.toString()).toString().trim()));
                            columns.add(col);
                        }
                    }
                    log.info("更新看板：传入DE调度的字段——————————");
                    log.info(columns.toString());
                    runtimeConfig.put("acrossCloud", "common");
                    runtimeConfig.put("alertMethod", new ArrayList<>());
                    runtimeConfig.put("alertType", new ArrayList<>());
                    runtimeConfig.put("batchParams", "");
                    runtimeConfig.put("clusterSla", "normal");
                    runtimeConfig.put("collaborators", new ArrayList<>());
                    runtimeConfig.put("columns", columns);
                    {
                        runtimeConfig.put("connectionUrl", biConfig.getUrl().get(qd.getRegion()));
                        runtimeConfig.put("dbUser", biConfig.getUsername().get(qd.getRegion()));
                        String password = encrypt(biConfig.getPassword().get(qd.getRegion()));
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("connectionUrl", cloudawsUrl);
                        runtimeConfig.put("dbUser", cloudawsUsername);
                        String password = encrypt(cloudawsPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                        runtimeConfig.put("dbUser", cloudawsSGUsername);
                        String password = encrypt(cloudawsSGPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                        runtimeConfig.put("dbUser", cloudhuaweiUsername);
                        String password = encrypt(cloudhuaweiPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    runtimeConfig.put("dsGroups", new ArrayList<>());
                    runtimeConfig.put("emails", "");
                    runtimeConfig.put("endDate", "");
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
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("sourceRegion", "ue1");
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("sourceRegion", "sg1");
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("sourceRegion", "sg2");
                    }
                    runtimeConfig.put("startDate", "");
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
                    addTaskValues.put("content", chartDetail.getContent());
                    String engine = chartDetail.getEngine();
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
                    addTaskValues.put("id",deTaskId);
                    String valuesForUpdateTask = JSONObject.toJSONString(addTaskValues);
                    log.info(String.format("%s update dashborad update crontab DE send %s",
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
                    log.info("更新看板：开启天调度结果——————————");
                    log.info(String.format("%s update dashborad update crontab DE get %s",
                            shareid, responseUpdateTaskBody));
                    JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                    if (!jsonUpdateTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("更新调度失败，调度id： %s",
                                deTaskId)));
                    }
                    TimeUnit.SECONDS.sleep(1);
                    //todo：上线任务
                    String onlineAndOffline = gatewayUrl+"/task/start?id=";
                    onlineAndOffline += deTaskId;
                    RequestBody body = RequestBody.create(mediaType, "");
                    Request requestOnlineTask = new Request.Builder()
                            .url(onlineAndOffline)
                            .method("PATCH",body)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
                    String responseOnlineTaskBody = responseOnlineTask.body().string();
                    log.info("更新dashboard：0->1;上线DE任务结果——————————");
                    log.info(responseOnlineTaskBody);
                    log.info(String.format("%s update dashborad online DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
                    JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
                    if (!jsonOnlineTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",deTaskId)));
                    }
                }
            }
            if (sche.equals(1) && isSche.equals(0)){
                for (Integer chid:chartList){
                    String uuid = chartMapper.getUuid(chid,1);
                    Integer deTaskId = queryDataMapper.selectDeTaskId(uuid,1);
                    //todo：调接口把这个deTaskId的调度关闭天粒度
                    String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
                    Request requestGetGroup = new Request.Builder()
                            .url(getGroupUrl)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseGetGroup = client.newCall(requestGetGroup).execute();
                    String responseGetGroupBody = responseGetGroup.body().string();
                    log.info(String.format("%s update dashboard get group info get %s",
                            shareid, responseGetGroupBody));
                    JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
                    JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
                    Integer groupId = (Integer) data.get("id");
                    List<Map<String,Object>> costList = new ArrayList<>();
                    Map<String,Object> cost = new HashMap<>();
                    cost.put("key", groupId);
                    cost.put("value", 100);
                    costList.add(cost);
                    Chart chartDetail = chartMapper.getChartForView(chid,1);
                    QueryData qd = queryDataMapper.getQd(uuid);
                    QueryHistory qh = queryHistoryMapper.selectByUuid(uuid);
                    JSONArray columnList = JSONArray.parseArray(qh.getColumnType());
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
                    log.info("更新看板：1->0 先下线任务——————————");
                    log.info(String.format("%s update dashborad offline DE [%s] get %s",
                            shareid, offLine, responseOffLineTaskBody));
                    TimeUnit.SECONDS.sleep(1);
                    //todo:修改crontab以及高级设置时间
                    String updateUrl = gatewayUrl+"/task/updateAndStart";
                    List<Map<String,Object>> partitions = new ArrayList<>();
                    Map<String,Object> addTaskValues = new HashMap<>();
                    String taskName = "bi_" + chid + "_dataset_" + shareid;
                    String uuidName = uuid.replace("-","_");
                    String taskDataName = "bi_"+uuidName+"_dataset_"+shareid;
                    Map<String,Object> runtimeConfig = new HashMap<>();
                    List<Map<String,Object>> columns = new ArrayList<>();
                    Map<String,Object> partitionsMap = new HashMap<>();
                    partitionsMap.put("name", "bidt");
                    partitionsMap.put("value", "{{ ds_nodash }}");
                    partitions.add(partitionsMap);

                    Map<String,Object> crontabParam = new HashMap<>();
                    crontabParam.put("endTime", "23:59");
                    crontabParam.put("fixedTime", "00:00");
                    crontabParam.put("interval", 5);
                    crontabParam.put("range", new ArrayList<>());
                    crontabParam.put("startTime", "00:00");

                    Map<String,Object> triggerParam = new HashMap<>();
                    triggerParam.put("crontab", "00 00 * * *");
                    triggerParam.put("outputGranularity", "daily");
                    triggerParam.put("type", "cron");
                    triggerParam.put("crontabParam", crontabParam);
                    String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartDetail.getQuerySql()).getBytes()));
                    //todo:加调度参数
                    Map<String,Object> typeUpMap = new HashMap<>();
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
                            Map<String,Object> col = new IdentityHashMap<>();
                            col.put(new String("columnName"), key.toString());
                            col.put(new String("columnType"), typeUpMap.get(colmap.get(key.toString()).toString().trim()));
                            columns.add(col);
                        }
                    }
                    runtimeConfig.put("acrossCloud", "common");
                    runtimeConfig.put("alertMethod", new ArrayList<>());
                    runtimeConfig.put("alertType", new ArrayList<>());
                    runtimeConfig.put("batchParams", "");
                    runtimeConfig.put("clusterSla", "normal");
                    runtimeConfig.put("collaborators", new ArrayList<>());
                    runtimeConfig.put("columns", columns);
                    {
                        runtimeConfig.put("connectionUrl", biConfig.getUrl().get(qd.getRegion()));
                        runtimeConfig.put("dbUser", biConfig.getUsername().get(qd.getRegion()));
                        String password = encrypt(biConfig.getPassword().get(qd.getRegion()));
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("connectionUrl", cloudawsUrl);
                        runtimeConfig.put("dbUser", cloudawsUsername);
                        String password = encrypt(cloudawsPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                        runtimeConfig.put("dbUser", cloudawsSGUsername);
                        String password = encrypt(cloudawsSGPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                        runtimeConfig.put("dbUser", cloudhuaweiUsername);
                        String password = encrypt(cloudhuaweiPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    runtimeConfig.put("dsGroups", new ArrayList<>());
                    runtimeConfig.put("emails", "");
                    runtimeConfig.put("endDate", "2010-11-10 23:59:59");
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
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("sourceRegion", "ue1");
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("sourceRegion", "sg1");
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("sourceRegion", "sg2");
                    }
                    runtimeConfig.put("startDate", "2010-11-10 00:00:00");
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
                    addTaskValues.put("content", chartDetail.getContent());
                    String engine = chartDetail.getEngine();
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
                    addTaskValues.put("id",deTaskId);
                    String valuesForUpdateTask = JSONObject.toJSONString(addTaskValues);
                    log.info(String.format("%s update dashborad close crontab send %s",
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
                    log.info("更新看板：关闭天调度结果——————————");
                    log.info(String.format("%s update dashborad close crontab get %s",
                            shareid, responseUpdateTaskBody));
                    JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                    if (!jsonUpdateTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("更新调度失败，调度id： %s",
                                deTaskId)));
                    }
                    TimeUnit.SECONDS.sleep(1);
                    //todo：上线任务
                    String onlineAndOffline = gatewayUrl+"/task/start?id=";
                    onlineAndOffline += deTaskId;
                    RequestBody body = RequestBody.create(mediaType, "");
                    Request requestOnlineTask = new Request.Builder()
                            .url(onlineAndOffline)
                            .method("PATCH",body)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
                    String responseOnlineTaskBody = responseOnlineTask.body().string();
                    log.info("更新dashboard：1->0;上线DE任务结果——————————");
                    log.info(String.format("%s update dashborad online DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
                    JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
                    if (!jsonOnlineTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",deTaskId)));
                    }
                }
            }
            if (sche.equals(1) && isSche.equals(1)){
                for (Integer chid:chartList){
                    String uuid = chartMapper.getUuid(chid,1);
                    Integer deTaskId = queryDataMapper.selectDeTaskId(uuid,1);
                    //todo：调接口把这个deTaskId的调度更新crontab时间
                    String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
                    Request requestGetGroup = new Request.Builder()
                            .url(getGroupUrl)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseGetGroup = client.newCall(requestGetGroup).execute();
                    String responseGetGroupBody = responseGetGroup.body().string();
                    log.info(String.format("%s update dashboard get group info get %s",
                            shareid, responseGetGroupBody));
                    JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
                    JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
                    Integer groupId = (Integer) data.get("id");
                    List<Map> costList = new ArrayList<>();
                    Map cost = new HashMap();
                    cost.put("key", groupId);
                    cost.put("value", 100);
                    costList.add(cost);
                    Chart chartDetail = chartMapper.getChartForView(chid,1);
                    QueryData qd = queryDataMapper.getQd(uuid);
                    QueryHistory qh = queryHistoryMapper.selectByUuid(uuid);
                    JSONArray columnList = JSONArray.parseArray(qh.getColumnType());
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
                    log.info("更新看板：1->1 先下线任务——————————");
                    log.info(String.format("%s update dashborad offline DE [%s] get %s",
                            shareid, offLine, responseOffLineTaskBody));
                    TimeUnit.SECONDS.sleep(1);
                    //todo:修改crontab以及高级设置时间
                    String updateUrl = gatewayUrl+"/task/updateAndStart";
                    List<Map> partitions = new ArrayList<>();
                    Map addTaskValues = new HashMap<>();
                    String taskName = "bi_" + chid + "_dataset_" + shareid;
                    String uuidName = uuid.replace("-","_");
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
                    triggerParam.put("crontab", map.get("crontab"));
                    triggerParam.put("outputGranularity", "daily");
                    triggerParam.put("type", "cron");
                    triggerParam.put("crontabParam", crontabParam);
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
                            columns.add(col);
                        }
                    }
                    runtimeConfig.put("acrossCloud", "common");
                    runtimeConfig.put("alertMethod", new ArrayList<>());
                    runtimeConfig.put("alertType", new ArrayList<>());
                    runtimeConfig.put("batchParams", "");
                    runtimeConfig.put("clusterSla", "normal");
                    runtimeConfig.put("collaborators", new ArrayList<>());
                    runtimeConfig.put("columns", columns);
                    {
                        runtimeConfig.put("connectionUrl", biConfig.getUrl().get(qd.getRegion()));
                        runtimeConfig.put("dbUser", biConfig.getUsername().get(qd.getRegion()));
                        String password = encrypt(biConfig.getPassword().get(qd.getRegion()));
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("connectionUrl", cloudawsUrl);
                        runtimeConfig.put("dbUser", cloudawsUsername);
                        String password = encrypt(cloudawsPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                        runtimeConfig.put("dbUser", cloudawsSGUsername);
                        String password = encrypt(cloudawsSGPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                        runtimeConfig.put("dbUser", cloudhuaweiUsername);
                        String password = encrypt(cloudhuaweiPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    runtimeConfig.put("dsGroups", new ArrayList<>());
                    runtimeConfig.put("emails", "");
                    runtimeConfig.put("endDate", "");
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
                    if (qd.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("sourceRegion", "ue1");
                    }
                    if (qd.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("sourceRegion", "sg1");
                    }
                    if (qd.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("sourceRegion", "sg2");
                    }
                    runtimeConfig.put("startDate", "");
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
                    addTaskValues.put("content", chartDetail.getContent());
                    String engine = chartDetail.getEngine();
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
                    addTaskValues.put("id",deTaskId);
                    String valuesForUpdateTask = JSONObject.toJSONString(addTaskValues);
                    log.info(String.format("%s update dashborad update crontab send %s",
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
                    log.info("更新看板：更新天调度crontab结果——————————");
                    log.info(String.format("%s update dashborad update crontab get %s",
                            shareid, responseUpdateTaskBody));
                    JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                    if (!jsonUpdateTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("更新调度失败，调度id： %s",
                                deTaskId)));
                    }
                    TimeUnit.SECONDS.sleep(1);
                    //todo：上线任务
                    String onlineAndOffline = gatewayUrl+"/task/start?id=";
                    onlineAndOffline += deTaskId;
                    RequestBody body = RequestBody.create(mediaType, "");
                    Request requestOnlineTask = new Request.Builder()
                            .url(onlineAndOffline)
                            .method("PATCH",body)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseOnlineTask = client.newCall(requestOnlineTask).execute();
                    String responseOnlineTaskBody = responseOnlineTask.body().string();
                    log.info("更新dashboard：1->0;上线DE任务结果——————————");
                    log.info(String.format("%s update dashborad online DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
                    JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
                    if (!jsonOnlineTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",deTaskId)));
                    }
                }
            }
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s update dashDE %s",
            		shareid, CommonUtil.printStackTraceToString(e)));
        }
    }

    @Override
    public void delDash(Integer id, String shareid, String token,String name,String tenantName){
        //todo:
        // 1、dashboard表isactive = 0，判断schedule是否为0，不是0则isschedule = 0，走下面这些步骤；是0就直接active完事
        // 2、拿id去dashchart表查所有的chartid；
        // 3、再拿每一个chartid查dashboardid（dashboard！=id）；
        // 4、拿dashboardid查到dashboard的schedule，都是0就更新调度时间2010，如果有1，不管；
        try {
            Dashboard dashboard = dashboardMapper.selectById(id,1);
            if (dashboard.getIsSchedule().equals(0)){
                dashboardMapper.updateByActive(id,0);
            }else {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");
                dashboardMapper.updateByActiveSche(id,0);
                List<Integer> chartIds = dashChartMapper.selectByIdDashId(id,1);
                for (Integer cid:chartIds){
                    List<Integer> daid = dashChartMapper.selectByIdChartDashId(cid, id,1);
                    List<Integer> scheList = new ArrayList<>();
                    for (Integer did:daid){
                        Integer sche = dashboardMapper.selectScheById(did,1);
                        scheList.add(sche);
                    }
                    if (!scheList.contains(1)){
                        String uuid = chartMapper.getUuid(cid,1);
                        Integer deTaskId = queryDataMapper.selectDeTaskId(uuid,1);
                        //todo：调接口把这个deTaskId的调度开启天粒度
                        String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
                        Request requestGetGroup = new Request.Builder()
                                .url(getGroupUrl)
                                .method("GET", null)
                                .addHeader("Authentication", token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response responseGetGroup = client.newCall(requestGetGroup).execute();
                        String responseGetGroupBody = responseGetGroup.body().string();
                        log.info(String.format("%s del dashboard get group info get %s",
                                shareid, responseGetGroupBody));
                        JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
                        JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
                        Integer groupId = (Integer) data.get("id");
                        List<Map> costList = new ArrayList<>();
                        Map cost = new HashMap();
                        cost.put("key", groupId);
                        cost.put("value", 100);
                        costList.add(cost);
                        Chart chartDetail = chartMapper.getChartForView(cid,1);
                        QueryData qd = queryDataMapper.getQd(uuid);
                        QueryHistory qh = queryHistoryMapper.selectByUuid(uuid);
                        JSONArray columnList = JSONArray.parseArray(qh.getColumnType());
                        String updateUrl = gatewayUrl+"/task/updateAndStart";
                        List<Map> partitions = new ArrayList<>();
                        Map addTaskValues = new HashMap<>();
                        String taskName = "bi_" + cid + "_dataset_" + shareid;
                        String uuidName = uuid.replace("-","_");
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
                        triggerParam.put("crontab", dashboard.getCrontab());
                        triggerParam.put("outputGranularity", "daily");
                        triggerParam.put("type", "cron");
                        triggerParam.put("crontabParam", crontabParam);
                        String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartDetail.getQuerySql()).getBytes()));
                        //todo:加调度参数
                        for (int i = 0; i < columnList.size(); i++) {
                            Map colmap = (Map) columnList.get(i);
                            for (Object key : colmap.keySet()) {
                                Map col = new IdentityHashMap();
                                col.put(new String("columnName"), key.toString());
                                col.put(new String("columnType"), colmap.get(key.toString()).toString());
                                columns.add(col);
                            }
                        }
                        runtimeConfig.put("acrossCloud", "common");
                        runtimeConfig.put("alertMethod", new ArrayList<>());
                        runtimeConfig.put("alertType", new ArrayList<>());
                        runtimeConfig.put("batchParams", "");
                        runtimeConfig.put("clusterSla", "normal");
                        runtimeConfig.put("collaborators", new ArrayList<>());
                        runtimeConfig.put("columns", columns);
                        {
                            runtimeConfig.put("connectionUrl", biConfig.getUrl().get(qd.getRegion()));
                            runtimeConfig.put("dbUser", biConfig.getUsername().get(qd.getRegion()));
                            String password = encrypt(biConfig.getPassword().get(qd.getRegion()));
                            runtimeConfig.put("dbPassword", password);
                        }
                        if (qd.getRegion().equals("aws_ue1")) {
                            runtimeConfig.put("connectionUrl", cloudawsUrl);
                            runtimeConfig.put("dbUser", cloudawsUsername);
                            String password = encrypt(cloudawsPassword);
                            runtimeConfig.put("dbPassword", password);
                        }
                        if (qd.getRegion().equals("aws_sg")) {
                            runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                            runtimeConfig.put("dbUser", cloudawsSGUsername);
                            String password = encrypt(cloudawsSGPassword);
                            runtimeConfig.put("dbPassword", password);
                        }
                        if (qd.getRegion().equals("huawei_sg")) {
                            runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                            runtimeConfig.put("dbUser", cloudhuaweiUsername);
                            String password = encrypt(cloudhuaweiPassword);
                            runtimeConfig.put("dbPassword", password);
                        }
                        runtimeConfig.put("dsGroups", new ArrayList<>());
                        runtimeConfig.put("emails", "");
                        runtimeConfig.put("endDate", "2010-11-10 23:59:59");
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
                        if (qd.getRegion().equals("aws_ue1")) {
                            runtimeConfig.put("sourceRegion", "ue1");
                        }
                        if (qd.getRegion().equals("aws_sg")) {
                            runtimeConfig.put("sourceRegion", "sg1");
                        }
                        if (qd.getRegion().equals("huawei_sg")) {
                            runtimeConfig.put("sourceRegion", "sg2");
                        }
                        runtimeConfig.put("startDate", "2010-11-10 00:00:00");
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
                        addTaskValues.put("content", chartDetail.getContent());
                        String engine = chartDetail.getEngine();
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
                        addTaskValues.put("id",deTaskId);
                        String valuesForUpdateTask = JSONObject.toJSONString(addTaskValues);
                        log.info(String.format("%s del dashboard close crontab send %s",
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
                        log.info("删除看板：关闭天调度结果——————————");
                        log.info(String.format("%s del dashboard close crontab get %s",
                                shareid, responseUpdateTaskBody));
                        JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                        if (!jsonUpdateTaskObj.get("code").equals(0)){
                            throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("更新调度失败，调度id： %s",
                                    deTaskId)));
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s del dash %s",
            		shareid, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(BaseResponseCodeEnum.valueOf(e.toString()));
        }
    }

    public void preCheckCommon(Dashboard dashboard, String name) {
        //1. name不重复校验
        String title = dashboard.getName();
        Integer id = dashboard.getId();
        Integer active = 1;
        List<String> existQuery = dashboardMapper.selectByUsername(title, name, id, active);
//        super.checkOnUpdate(super.getByName(savedQuery.getTitle()), savedQuery);
        if (existQuery.contains(title)) {
            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
        }
    }

    @Override
    public void addDash(HashMap<String, String > map, String shareid, Integer id, String token, String name, String tenantName){
        //todo:
        // 2、拿到每个chartid，往dashchart表挨个新增；
        // 3、判断dashboard的sche，如果是0，不管；如果是1，把这几个chart对应的uuid都更新时间；
        try {
            LocalDateTime time = LocalDateTime.now();
            List<Object> list = JSONArray.parseArray(JSON.toJSONString(map.get("chart_id")));
            for (Object object : list){
                Integer chartId = Integer.valueOf(object.toString());
                dashChartMapper.addDashChart(id,chartId,1,name,time);
            }
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s add dash %s",
            		shareid, CommonUtil.printStackTraceToString(e)));
            throw e;
        }
    }

    @Override
    @Async
    public void addDashDE(HashMap<String, String > map, String shareid, Integer id, String token, String name, String tenantName) {
        //todo:
        // 2、拿到每个chartid，往dashchart表挨个新增；
        // 3、判断dashboard的sche，如果是0，不管；如果是1，把这几个chart对应的uuid都更新时间；
        try {
            InfTraceContextHolder.get().setTenantName(tenantName);
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            Integer sche = Integer.parseInt(map.get("is_schedule"));
            List<Object> list = JSONArray.parseArray(JSON.toJSONString(map.get("chart_id")));
            if (sche.equals(1)){
                for (Object object : list){
                    Integer chartId = Integer.parseInt(object.toString());
                    String getGroupUrl = gatewayUrl+"/user/getEffectiveCostList?name=" + shareid;
                    Request requestGetGroup = new Request.Builder()
                            .url(getGroupUrl)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseGetGroup = client.newCall(requestGetGroup).execute();
                    String responseGetGroupBody = responseGetGroup.body().string();
                    log.info(String.format("%s add dashboard get group info get %s",
                            shareid, responseGetGroupBody));
                    JSONObject jsonGetGroupObj = JSONObject.parseObject(responseGetGroupBody);
                    JSONObject data = (JSONObject) jsonGetGroupObj.getJSONArray("data").get(0);
                    Integer groupId = (Integer) data.get("id");
                    List<Map> costList = new ArrayList<>();
                    Map cost = new HashMap();
                    cost.put("key", groupId);
                    cost.put("value", 100);
                    costList.add(cost);
                    Chart chartDetail = chartMapper.getChartForView(chartId,1);
                    String uuid = chartMapper.selectUuidById(chartId,name);
                    Integer taskId = queryDataMapper.selectDeTaskId(uuid,1);
                    QueryData qd = queryDataMapper.getQd(uuid);
                    QueryHistory qh = queryHistoryMapper.selectByUuid(uuid);
                    JSONArray columnList = JSONArray.parseArray(qh.getColumnType());
                    //todo：先下线任务
                    String offLine = gatewayUrl+"/task/onlineAndOffline?id=";
                    offLine = offLine+taskId+"&status=0&ifnotify=false";
                    RequestBody offLineBody = RequestBody.create(mediaType, "");
                    Request requestOffLineTask = new Request.Builder()
                            .url(offLine)
                            .method("PUT", offLineBody)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseOffLineTask = client.newCall(requestOffLineTask).execute();
                    String responseOffLineTaskBody = responseOffLineTask.body().string();
                    log.info("更新看板：0->1 先下线任务——————————");
                    log.info(String.format("%s add dashboard offline DE [%s] get %s",
                            shareid, offLine, responseOffLineTaskBody));
                    TimeUnit.SECONDS.sleep(1);
                    //todo:更新crontab时间以及高级设置
                    String updateUrl = gatewayUrl+"/task/updateAndStart";
                    List<Map> partitions = new ArrayList<>();
                    Map addTaskValues = new HashMap<>();
                    String taskName = "bi_" + chartId + "_dataset_" + shareid;
                    String uuidName = uuid.replace("-","_");
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
                    triggerParam.put("crontab", map.get("crontab"));
                    triggerParam.put("outputGranularity", "daily");
                    triggerParam.put("type", "cron");
                    triggerParam.put("crontabParam", crontabParam);
                    String encodedSql = new String(Base64.getEncoder().encode(URLEncoder.encode(chartDetail.getQuerySql()).getBytes()));
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
                    runtimeConfig.put("acrossCloud", "common");
                    runtimeConfig.put("alertMethod", new ArrayList<>());
                    runtimeConfig.put("alertType", new ArrayList<>());
                    runtimeConfig.put("batchParams", "");
                    runtimeConfig.put("clusterSla", "normal");
                    runtimeConfig.put("collaborators", new ArrayList<>());
                    runtimeConfig.put("columns", columns);
                    {
                        runtimeConfig.put("connectionUrl", biConfig.getUrl().get(qd.getRegion()));
                        runtimeConfig.put("dbUser", biConfig.getUsername().get(qd.getRegion()));
                        String password = encrypt(biConfig.getPassword().get(qd.getRegion()));
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qh.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("connectionUrl", cloudawsUrl);
                        runtimeConfig.put("dbUser", cloudawsUsername);
                        String password = encrypt(cloudawsPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qh.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("connectionUrl", cloudawsSGUrl);
                        runtimeConfig.put("dbUser", cloudawsSGUsername);
                        String password = encrypt(cloudawsSGPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    if (qh.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("connectionUrl", cloudhuaweiUrl);
                        runtimeConfig.put("dbUser", cloudhuaweiUsername);
                        String password = encrypt(cloudhuaweiPassword);
                        runtimeConfig.put("dbPassword", password);
                    }
                    runtimeConfig.put("dsGroups", new ArrayList<>());
                    runtimeConfig.put("emails", "");
                    runtimeConfig.put("endDate", "");
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
                    if (qh.getRegion().equals("aws_ue1")) {
                        runtimeConfig.put("sourceRegion", "ue1");
                    }
                    if (qh.getRegion().equals("aws_sg")) {
                        runtimeConfig.put("sourceRegion", "sg1");
                    }
                    if (qh.getRegion().equals("huawei_sg")) {
                        runtimeConfig.put("sourceRegion", "sg2");
                    }
                    runtimeConfig.put("startDate", "");
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
                    addTaskValues.put("content", chartDetail.getContent());
                    String engine = chartDetail.getEngine();
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
                    addTaskValues.put("id",taskId);
                    String valuesForUpdateTask = JSONObject.toJSONString(addTaskValues);
                    log.info(String.format("%s add dashboard update crontab send %s",
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
                    log.info("新增看板：DE更新调度返回————————————");
                    log.info(String.format("%s add dashboard update crontab get %s",
                            shareid, responseUpdateTaskBody));
                    JSONObject jsonUpdateTaskObj = JSONObject.parseObject(responseUpdateTaskBody);
                    if (!jsonUpdateTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("更新调度失败，调度id： %s",
                                taskId)));
                    }
                    TimeUnit.SECONDS.sleep(1);
                    //todo：上线任务
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
                    log.info("新增dashboard：上线DE任务结果——————————");
                    log.info(String.format("%s add dashboard online DE [%s] get %s",
                            shareid, onlineAndOffline, responseOnlineTaskBody));
                    JSONObject jsonOnlineTaskObj = JSONObject.parseObject(responseOnlineTaskBody);
                    if (!jsonOnlineTaskObj.get("code").equals(0)){
                        throw new ServiceException(BaseResponseCodeEnum.valueOf(String.format("上线失败，调度id： %s",taskId)));
                    }
                }
            }
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s add dashDE %s",
            		shareid, CommonUtil.printStackTraceToString(e)));
        }
    }

    @Override
    public List<Dashboard> getDash(Integer id){
        return dashboardMapper.selectDash(id,1);
    }

    @Override
    public List<Chart> getChart(Integer id,String token, String name) throws IOException {
        List<Chart> chart = new ArrayList<>();
        List<Integer> chartIds = dashChartMapper.selectByIdDashId(id,1);
        for (Integer cid:chartIds){
            List sche = new ArrayList();
            Chart ch = chartMapper.getChartForView(cid,1);
            chart.add(ch);
            /* QueryData qd = queryDataMapper.getQd(ch.getUuid());
            List<DashChart> dachList = dashChartMapper.selectByChartId(ch.getId(),name,1);
            for (DashChart dach:dachList){
                Integer dashsche = dashboardMapper.selectDashById(dach.getDashboardId(),name,1);
                sche.add(dashsche);
            }
//            QueryData qd = queryDataMapper.getQd(ch.getUuid());
            if (sche.contains(1)){
                if (null != qd) {
                    LocalDate dt = LocalDate.now();
                    String url = gatewayUrl + "/taskinstance/page?taskId=" + qd.getDetaskid() + "&name=" + qd.getDb() + "&pageNum=1&pageSize=1&start_date=" + dt + "+00:00:00&end_date=" + dt + "+23:59:59";
                    Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseTask = client.newCall(requestTask).execute();
                    String responseTaskBody = responseTask.body().string();
                    JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                    JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                    JSONObject shareidListResult = shareidListData.getJSONObject("result");
                    JSONArray shareidList = shareidListResult.getJSONArray("list");
                    JSONObject status = (JSONObject) shareidList.get(0);
                    chartMapper.updateStatus(cid, status.get("state").toString());
                }
                chart.add(ch);
            }
            else{
                if (!ch.getStatus().equals("success") && null != qd){
                    /* String url = gatewayUrl+"/taskinstance/page?taskId="+qd.getDetaskid()+"&name="+qd.getDb()+"&pageNum=1&pageSize=1";
                    Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseTask = client.newCall(requestTask).execute();
                    String responseTaskBody = responseTask.body().string();
                    JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                    JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                    JSONObject shareidListResult = shareidListData.getJSONObject("result");
                    JSONArray shareidList = shareidListResult.getJSONArray("list");
                    JSONObject status = (JSONObject)shareidList.get(0);
                    chartMapper.updateStatus(cid,status.get("state").toString());
                    chart.add(ch);
                }
                else {
                    chart.add(ch);
                }
            } */
        }
        return chart;
    }

    @Override
    @Async
    public void updateChartStatus(Integer id, String token, String name, String tenantName) {
        try {
            InfTraceContextHolder.get().setTenantName(tenantName);
            OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
            List<Integer> chartIds = dashChartMapper.selectByIdDashId(id,1);
            for (Integer cid : chartIds) {
                List sche = new ArrayList();
                Chart ch = chartMapper.getChartForView(cid, 1);
                QueryData qd = queryDataMapper.getQd(ch.getUuid());
                List<DashChart> dachList = dashChartMapper.selectByChartId(ch.getId(), name, 1);
                for (DashChart dach : dachList) {
                    Integer dashsche = dashboardMapper.selectDashById(dach.getDashboardId(), name, 1);
                    sche.add(dashsche);
                }
                if (sche.contains(1)) {
                    if (null == qd) {
                        log.info(String.format("%s update chart status %d query data is null",
                                name, cid));
                        continue;
                    }
                    LocalDate dt = LocalDate.now();
                    String url = gatewayUrl + "/taskinstance/page?taskId=" + qd.getDetaskid() + "&name=" + qd.getDb() + "&pageNum=1&pageSize=1&start_date=" + dt + "+00:00:00&end_date=" + dt + "+23:59:59";
                    Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response responseTask = client.newCall(requestTask).execute();
                    String responseTaskBody = responseTask.body().string();
                    log.info(String.format("%s update chart status %d [%s] get %s",
                            name, ch.getId(), url, responseTaskBody));
                    JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                    JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                    JSONObject shareidListResult = shareidListData.getJSONObject("result");
                    JSONArray shareidList = shareidListResult.getJSONArray("list");
                    JSONObject status = (JSONObject) shareidList.get(0);
                    chartMapper.updateStatus(cid, status.get("state").toString());
                    log.info(String.format("%s update chart status %d to %s",
                            name, cid, status.get("state").toString()));
                } else {
                    if (!ch.getStatus().equals("success") && null != qd) {
                        String url = gatewayUrl + "/taskinstance/page?taskId=" + qd.getDetaskid() + "&name=" + qd.getDb() + "&pageNum=1&pageSize=1";
                        Request requestTask = new Request.Builder()
                            .url(url)
                            .method("GET", null)
                            .addHeader("Authentication", token)
                            .addHeader("Content-Type", "application/json")
                            .build();
                        Response responseTask = client.newCall(requestTask).execute();
                        String responseTaskBody = responseTask.body().string();
                        log.info(String.format("%s update chart status %d [%s] get %s",
                                name, ch.getId(), url, responseTaskBody));
                        JSONObject jsonTaskObj = JSONObject.parseObject(responseTaskBody);
                        JSONObject shareidListData = jsonTaskObj.getJSONObject("data");
                        JSONObject shareidListResult = shareidListData.getJSONObject("result");
                        JSONArray shareidList = shareidListResult.getJSONArray("list");
                        JSONObject status = (JSONObject) shareidList.get(0);
                        chartMapper.updateStatus(cid, status.get("state").toString());
                        log.info(String.format("%s update chart status %d to %s",
                                name, cid, status.get("state").toString()));
                    }
                }
            }
        } catch (Exception e){
                log.error(String.format("There is a stack err when %s update chart status %s",
                        name, CommonUtil.printStackTraceToString(e)));
        }
    }

    @Override
    public List getTree(String name){
        List<ClassificationDash> classiByName = classificationDashMapper.classiByname(name,1);
        classiByName.sort(Comparator.comparing(ClassificationDash::getName));
        List<Dashboard> dashboard = dashboardMapper.classiByname(name, 1);
        dashboard.sort(Comparator.comparing(Dashboard::getName));
        List sourceTree = new ArrayList<>();
        List child = new ArrayList<>();
        for (int i=0;i<classiByName.size();i++){
            Map source = new HashMap();
            source.put("id",classiByName.get(i).getId());
            source.put("name",classiByName.get(i).getName());
            source.put("parent",classiByName.get(i).getParentId());
            source.put("level",classiByName.get(i).getLevel());
            source.put("child",child);
            source.put("type","目录");
            source.put("isFavorate",0);
            source.put("isShare",0);
            sourceTree.add(source);
        }
        for (int i=0;i<dashboard.size();i++){
            Map source = new HashMap();
            source.put("id",dashboard.get(i).getId());
            source.put("name",dashboard.get(i).getName());
            source.put("parent",dashboard.get(i).getClassId());
            source.put("level",null);
//            source.put("child",child);
            source.put("type","看板");
            source.put("isFavorate",dashboard.get(i).getIsFavorate());
            source.put("isShare",0);
            sourceTree.add(source);
        }
//        Integer parentId = classificationDashMapper.getIdByCreate(name,0,1);
//        List<Integer> getShare = shareMapper.getShareChart(name,"dashboard");
//        for (Integer id:getShare){
//            Map source = new HashMap();
//            Dashboard share = dashboardMapper.selectById(id,1);
//            if (share!=null){
//                String shareName = share.getName();
//                Integer shareId = share.getId();
//                source.put("id",shareId);
//                source.put("name",shareName);
//                source.put("parent",parentId);
//                source.put("level",null);
//                source.put("child",child);
//                source.put("type","看板");
//                source.put("isFavorate",0);
//                source.put("isShare",1);
//                sourceTree.add(source);
//            }
//        }
        List<Map<String,Object>> tree = generate_tree(sourceTree,null);
        return tree;
    }

    public List<Map<String,Object>> generate_tree(List sourceTree,Integer parent){
        List<Map<String,Object>> tree = new ArrayList();
        for (int i=0;i<sourceTree.size();i++){
            Map<String,Object> item = (Map<String, Object>) sourceTree.get(i);
//            boolean a = item.get("parent").equals(parent);
            if (parent == null){
                if (item.get("parent")==parent){
                    List child = generate_tree(sourceTree,Integer.parseInt(item.get("id").toString()));
                    item.put("child",child);
                    tree.add(item);
                }
            }else {
                if (item.get("parent")!=null){
                    if (item.get("parent").equals(parent)){
                        if (item.get("type").equals("目录")){
                            List child = generate_tree(sourceTree,Integer.parseInt(item.get("id").toString()));
                            item.put("child",child);
                            tree.add(item);
                        }else {
//                            item.put("child",item);
                            tree.add(item);
                        }
                    }
                }
            }
        }
        return tree;
    }

    @Override
    public PageInfo<List> getList(String type, String name, Integer pageNum, Integer pageSize){
        List dash = new ArrayList();
        if (type.equals("my")){
            List<Dashboard> chartList = dashboardMapper.getDashByCreat(name,1);
            for (Dashboard dh:chartList){
                dash.add(dh);
            }
        }
        if (type.equals("tuck")){
            List<Integer> favorChart = favorDashChartMapper.getFavorDash(name, "dashboard", 1);
            for (Integer id:favorChart){
                dash.add(dashboardMapper.selectById(id,1));
            }
        }
        if (type.equals("share")){
            List<Integer> shareChart = shareMapper.getShareDash(name,"dashboard");
            for (Integer id:shareChart){
                dash.add(dashboardMapper.selectById(id,1));
            }
        }
        Integer t = (int) new PageInfo<>(dash).getTotal();
        PageHelper.startPage(pageNum, pageSize);
        List dashResult = new ArrayList();
        if (type.equals("my")){
            List<Dashboard> chartList = dashboardMapper.getDashByCreat(name,1);
            for (Dashboard dh:chartList){
                dashResult.add(dh);
            }
        }
        if (type.equals("tuck")){
            List<Integer> favorChart = favorDashChartMapper.getFavorDash(name, "dashboard", 1);
            for (Integer id:favorChart){
                Dashboard dh = dashboardMapper.selectById(id,1);
                if (dh!=null){
                    dh.setIsFavorate(1);
                    dashResult.add(dh);
                }
            }
        }
        if (type.equals("share")){
            List<Integer> shareChart = shareMapper.getShareDash(name,"dashboard");
            for (Integer id:shareChart){
                Dashboard dh = dashboardMapper.selectById(id,1);
                if (dh!=null){
                    dh.setIsShare(1);
                    dashResult.add(dh);
                }
            }
        }
        PageInfo pageInfo = new PageInfo<>(dashResult);
        pageInfo.setTotal(t);
        return pageInfo;
    }

    @Override
    public void addDashLog(String name, Integer id){
        LocalDateTime time = LocalDateTime.now();
        logViewMapper.addLog(name, id, time, "dashboard");
    }

    @Override
    public void addFirstLevel(String name){
        Integer active = 1;
        Integer level = 0;
        String title = "全部文件夹";
        Integer parent = null;
        LocalDateTime createTime = LocalDateTime.now();
        List<Classification> selectByName = classificationDashMapper.selectByNameTitle(name,active,level,title);
        if (selectByName.size()==0){
            boolean addLevelFirst = classificationDashMapper.addLevelFirst(name,active,level,title,createTime,parent);
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
    public List<Integer> getAuth(String name){
        List<Dashboard> dashboard = dashboardMapper.getDashByCreat(name,1);
        List<Integer> share = shareMapper.getShareDash(name,"dashboard");
        List<Integer> auth = new ArrayList<>();
        for (Integer id:share){
            auth.add(id);
        }
        for (Dashboard did:dashboard){
            auth.add(did.getId());
        }
        return auth;
    }

}
