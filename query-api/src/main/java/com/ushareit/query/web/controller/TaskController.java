package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.GatewayService;
import com.ushareit.query.service.TaskService;
import com.ushareit.query.web.utils.EncryptUtil;
import com.ushareit.query.web.vo.BaseResponse;
import com.ushareit.query.bean.CurrentUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author: tianxu
 * @createDate: 2022-02-10
 * description: 执行sql任务
 */
@Slf4j
@Api(tags = "执行查询任务")
@RestController
@EnableAsync
@RequestMapping("/query")
public class TaskController extends BaseBusinessController<QueryHistory>{

    @Autowired
    private TaskService taskService;
    @Autowired
    private GatewayService gatewayService;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Override
    public BaseService<QueryHistory> getBaseService() {
        return (BaseService<QueryHistory>) taskService;
    }

    @ApiOperation("查询sql")
    @PostMapping(value = "/execute")
    public BaseResponse<?> execute(@RequestHeader("Authentication") String token, @RequestBody @Valid String params) throws ParseException, SQLException {
        HashMap<String, Object> response = new HashMap<>();
        HashMap<String, Object > map = JSON.parseObject(params, HashMap.class);
        CurrentUser curUser;
        if(null!=map.get("isSysCall") && (Integer) map.get("isSysCall")==1){
            JSONObject jo = (JSONObject) map.get("curUser");
            curUser = jo.toJavaObject(CurrentUser.class) ;
        }else {
            curUser = getCurrentUser();
        }
        String user = curUser.getUserName();
        String strGIds = curUser.getGroupIds();
        String groupId = null;
        if (null != strGIds) {
            String groupIds[] = curUser.getGroupIds().split(",");
            groupId = groupIds[0];
        }
        int tenantId = curUser.getTenantId();
        String tenantName = curUser.getTenantName();

        String engine = map.get("engine").toString();
        String uuid = map.get("uuid").toString();
        Integer isDatabend = Integer.parseInt(map.get("isDatabend").toString());
        Integer confirmedSmart = 0;
        if (null != map.get("confirmedSmart")) {
        	confirmedSmart = Integer.parseInt(map.get("confirmedSmart").toString());
        }
        String region = "";
        String catalog = "";
        log.info(String.format("execute stage-record begin uuid %s", uuid));
        if (null != map.get("region") && null != map.get("catalog")) {
            region = map.get("region").toString();
            catalog = map.get("catalog").toString().toLowerCase();
            if (null != region && region.trim().length() > 0) {
        	    if (catalog.equalsIgnoreCase("hive") || catalog.equalsIgnoreCase("iceberg")) {
            		if (region.equalsIgnoreCase("aws_ue1")) {
            			engine = "presto_aws";
            		} else if (region.equalsIgnoreCase("aws_sg")) {
        	    		engine = "presto_aws_sg";
        		    } else {
        			    engine = "presto_huawei";
        		    }
        	    } else {
        		    engine = catalog;
        	    }
            }
        }

        Integer isAsync = taskService.getMysqlAsync(engine);
        log.info(String.format("execute stage-record getMysqlAsync success uuid %s", uuid));

        /*if (engine.startsWith("mysql") && isAsync == 0 && isDatabend == 1) {
            taskService.executeMysqlAsyn(map.get("uuid").toString(), map.get("engine").toString(),
            		map.get("querySql").toString().trim(), map.get("querySqlParam").toString(),
            		JSONObject.parseObject(map.get("param").toString()),
            		user, region, catalog, groupId, tenantId, tenantName);
            log.info(String.format("execute stage-record executeMysqlAsyn success uuid %s", uuid));
            response = taskService.executeMysqlSample(map.get("uuid").toString(), map.get("engine").toString(),
            		map.get("querySql").toString().trim(), user);
            log.info(String.format("execute stage-record executeMysqlSample success uuid %s", uuid));
            if (response.get("code").equals(0) || response.get("code").equals(2)) {
                if (response.get("code").equals(0)) {
                    HashMap<String, Object> data = (HashMap<String, Object>) response.get("data");
                    JSONArray type = JSONObject.parseArray(JSONObject.toJSONString(data.get("type")));
                    JSONArray result = JSONObject.parseArray(JSONObject.toJSONString(data.get("result")));
                    if (result != null) {
                        if (result.size() > 0) {
                            JSONObject sample = result.getJSONObject(0);
                            taskService.probeAsyn(uuid, user, type, false, sample, tenantName);
                            log.info(String.format("execute stage-record probeAsyn success uuid %s", uuid));
                        }
                    }
                }
                log.info(String.format("execute stage-record end uuid %s", uuid));
                return BaseResponse.success(response.get("data"));
            } else {
                if (null == response.get("errorType") || null == response.get("errorZh")) {
                    log.info(String.format("execute stage-record end uuid %s", uuid));
                    return BaseResponse.error(response.get("code").toString(),
                    		response.get("message").toString(),
                    		Integer.parseInt(response.get("data").toString()));
                } else {
                    log.info(String.format("execute stage-record end uuid %s", uuid));
                    return BaseResponse.error(response.get("code").toString(),
                            response.get("message").toString(),
                            Integer.parseInt(response.get("data").toString()),
                            response.get("errorType").toString(),
                            response.get("errorZh").toString());
                }
            }

        } else*/ {
            /*if (0 == confirmedSmart && map.get("engine").toString().startsWith("presto")) {
                if (region.equals("aws_ue1") || region.equals("aws_sg")
                        || region.equals("huawei_sg")) {
                    response = taskService.checkSmartEngine(map.get("uuid").toString(),
                            map.get("querySql").toString().trim(), user, region);
                    if (null != response.get("code") && response.get("code").equals(1001)) {
                        return BaseResponse.error(response.get("code").toString(),
                                response.get("message").toString(),
                                Integer.parseInt(response.get("data").toString()));
                    }
                }
            }*/
            String querySql = map.get("querySql").toString().trim();
            if (1 == confirmedSmart && engine.startsWith("spark")) {
                Map<String, String> statusObj = taskService.transSQLtoSpark(querySql, user);
                if (statusObj.get("res").equals("SUCCESS")) {
                    querySql = statusObj.get("sparkSql");
                }
            }
            String database = "";
            if (null != map.get("database")) {
                database = map.get("database").toString();
            }

            String executionDate = "";
            JSONObject paramObject = JSONObject.parseObject(map.get("param").toString());
            if (paramObject != null && paramObject.size() > 0 && StringUtils.isNotEmpty(paramObject.getString("execution_date"))) {
                executionDate = paramObject.getString("execution_date").trim();
            }
            /*if (map.get("engine").toString().startsWith("hive")) {
                database = curUser.getDefaultHiveDbName();
            }*/
            Integer taskId = (Integer) map.get("taskId");
            boolean isDDL = querySql.trim().toLowerCase().startsWith("create") || querySql.trim().toLowerCase().startsWith("show");
            boolean isTrinoOlap = map.get("engine").toString().startsWith("presto")
                    && (region.equals("huawei_sg") || !taskService.isInExpWhitelist(1, user));
            boolean isSparkOlap = map.get("engine").toString().startsWith("spark")
                    && region.equals("huawei_sg");
            if (false) {
                response = taskService.execute(map.get("uuid").toString(), map.get("engine").toString(),
                        querySql, isDatabend, confirmedSmart,
                        user, region, catalog, database, groupId, tenantId, tenantName, taskId);
            } else {
                try {
                    response = gatewayService.execute(map.get("uuid").toString(), map.get("engine").toString(),
                            querySql, region, catalog, database, curUser, token, getUserInfo(), taskId, executionDate);
                } catch (Exception e) {
                    return BaseResponse.error("500", e.getMessage());
                }
            }
            log.info(String.format("execute stage-record execute success uuid %s", uuid));
            if (response.get("code").equals(0) || response.get("code").equals(2)){
                if (response.get("code").equals(0)) {
                    HashMap<String, Object> data = (HashMap<String, Object>) response.get("data");
                    JSONArray type = JSONObject.parseArray(JSONObject.toJSONString(data.get("type")));
                    JSONArray result = JSONObject.parseArray(JSONObject.toJSONString(data.get("result")));
                    if (result != null) {
                        if (result.size() > 0) {
                            JSONObject sample = result.getJSONObject(0);
                            //taskService.probeAsyn(map.get("uuid").toString(), user, type,false, sample, tenantName);
                            log.info(String.format("execute stage-record probeAsyn success uuid %s", uuid));
                        }
                    }
                }
                taskService.saveQuerySqlParam(map.get("uuid").toString(),
                		map.get("querySqlParam").toString(),
                		JSONObject.parseObject(map.get("param").toString()));
                log.info(String.format("execute stage-record saveQuerySqlParam success uuid %s", uuid));
                log.info(String.format("execute stage-record end uuid %s", uuid));
                return BaseResponse.success(response.get("data"));
            } else {
                taskService.saveQuerySqlParam(map.get("uuid").toString(),
                		map.get("querySqlParam").toString(),
                		JSONObject.parseObject(map.get("param").toString()));
                log.info(String.format("%s saveQuerySqlParam successfully when failed to execute query[uuid=%s]", user, uuid));
                if (null == response.get("errorType") || null == response.get("errorZh")) {
                    log.info(String.format("execute stage-record end uuid %s", uuid));
                    return BaseResponse.error(response.get("code").toString(),
                    		response.get("message").toString(),
                            response.get("data") != null ? Integer.parseInt(response.get("data").toString()) : -1);
                } else {
                    log.info(String.format("execute stage-record end uuid %s", uuid));
                    return BaseResponse.error(response.get("code").toString(),
                            response.get("message").toString(),
                            Integer.parseInt(response.get("data").toString()),
                            response.get("errorType").toString(),
                            response.get("errorZh").toString());
                }
            }
        }
    }

    @ApiOperation("取消查询")
    @GetMapping(value = "/cancel")
    public BaseResponse<?> cancel(@RequestParam String uuid) throws IOException, InterruptedException {
        String user = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            HashMap<String, Object> result = new HashMap<>();
            int code = 21;
            String message = "failed to get queryId";
            result.put("status", code);
            result.put("message", String.format("[uuid=%s]%s", uuid, message));
            log.error(String.format("%s %s when cancel task[uuid=%s, code=%d]", user, message, uuid, code));
            return BaseResponse.success(result);
        }
        if (gatewayService.fromOlap(uuid)) {
            taskService.cancel(uuid, user, tenantName);
        } else {
            gatewayService.cancel(uuid, user, tenantName);
        }
        HashMap<String, Object> result = new HashMap<>();
        result.put("status", 2);
        result.put("message", "已取消");
        return BaseResponse.success(result);
        /* HashMap<String, Object> response = taskService.cancel(uuid, user);
        if (response.get("code").equals(0) || response.get("code").equals(26)) {
            HashMap<String, Object> data = (HashMap<String, Object>) response.get("data");

            if (response.get("code").equals(26)) {
                taskService.cancelAsync((Map<String, String>) data.get("info"));
            }
            HashMap<String, Object> result = new HashMap<>();
            result.put("status", Integer.parseInt(String.valueOf(data.get("status"))));
            result.put("message", String.valueOf(data.get("message")));
            return BaseResponse.success(result);
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        } */
    }

    @GetMapping(value="/queryId")
    public BaseResponse<?> getQueryId(@RequestParam String uuid) {
        return BaseResponse.success(gatewayService.getQueryId(uuid));
    }

    @ApiOperation("下载历史数据")
    @GetMapping(value = "/download")
    public ResponseEntity<Object> download(@RequestHeader("Authentication") String token,
                                           @RequestParam @Valid String uuid,
                                           @RequestParam(defaultValue = "csv") @Valid String type) throws Exception {
        CurrentUser curUser = getCurrentUser();
        String user = curUser.getUserName();
        if (!type.equals("csv") && !type.equals("pdf")) {
            String message = String.format("invalid file type %s", type);
            log.error(String.format("[%s]%s", user, message));
            throw new ServiceException(message, message);
        }
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            String message = String.format("There is no uuid[%s] info when downloading", uuid);
            log.error(String.format("[%s]%s", user, message));
            throw new ServiceException(message, message);
        }
        if (gatewayService.fromOlap(uuid)) {
            return taskService.download(uuid, user);
        } else {
            if (type.equals("pdf")) {
                return gatewayService.downloadPdf(uuid, user, getUserInfo(), String.valueOf(queryHistory.getId()), queryHistory);
            } else {
                return gatewayService.download(uuid, user, getUserInfo(), String.valueOf(queryHistory.getId()), queryHistory);
            }
        }
    }

    @ApiOperation("下载历史数据")
    @GetMapping(value = "/downloadFile")
    public BaseResponse<?> downloadFile(@RequestParam @Valid String uuid) throws Exception {
        String user = getCurrentUser().getUserName();
        try {
            List response = gatewayService.downloadLink(uuid, user, getUserInfo());
            return BaseResponse.success(response);
        } catch (Exception e) {
            return BaseResponse.error("500", e.getMessage());
        }
    }

    @ApiOperation("查询历史任务的sql")
    @GetMapping(value = "/history/sql")
    public BaseResponse<?> historySql(@RequestParam @Valid String uuid) throws Exception {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> response = taskService.historySql(uuid, user);
        if (response.get("code").equals(0)){
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("查询历史任务的数据")
    @GetMapping(value = "/history/data")
    public BaseResponse<?> history(@RequestParam @Valid String uuid) throws Exception {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> response = taskService.historyData(uuid, user);
        if (response.get("code").equals(0)){
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("获取spark查询的日志url")
    @GetMapping(value = "/logs")
    public BaseResponse<?> queryLog(@RequestParam @Valid String uuid,
                                    @RequestParam @Valid Long from) throws Exception {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> response = taskService.queryNewLog(uuid, user, from);
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("获取spark查询的日志url")
    @GetMapping(value = "/logsText")
    public BaseResponse<?> queryLogText(@RequestParam @Valid String uuid,
                                        @RequestParam(defaultValue = "0") Integer from,
                                        @RequestParam(defaultValue = "100") Integer size) throws Exception {
        String user = getCurrentUser().getUserName();
        Map<String, Object> response = gatewayService.queryLogText(uuid, from, size, user);
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("已保存查询的分享")
    @GetMapping(value="/share")
    public BaseResponse<?> shareId(@RequestParam @Valid Integer ids) {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> response = taskService.shareId(ids, user);
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("获取mysql异步查询结束后的下载量")
    @GetMapping(value="/fileSize")
    public BaseResponse<?> fileSize(@RequestParam @Valid String uuid) {
        String user = getCurrentUser().getUserName();
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (null == queryHistory) {
            String message = String.format("There is no uuid[%s] info when get file size", uuid);
            log.error(String.format("[%s]%s", user, message));
            return BaseResponse.error("41", message);
        }
        HashMap<String, Object> response;
        if (gatewayService.fromOlap(uuid)) {
            response = taskService.getFileSize(uuid, user);
        } else {
            response = gatewayService.getFileSize(uuid, user, getUserInfo());
        }
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("获取uuid对应的任务状态")
    @PostMapping(value="/getStatus")
    public BaseResponse<?> getStatus(@RequestBody @Valid String params) {
        HashMap<String, Object > map = JSON.parseObject(params, HashMap.class);

        JSONArray uuidList = JSONObject.parseArray(map.get("uuidList").toString());

        HashMap<String, Object> statusObj = taskService.getStatus(uuidList);

        return BaseResponse.success(statusObj);
    }

    @ApiOperation("获取分享权限")
    @GetMapping(value="/shareGrade")
    public BaseResponse<?> getShare(@RequestParam(defaultValue = "") String sharee,
            @RequestParam(defaultValue = "0") Integer gradeID) {
    	if (0 == gradeID) {
    		return BaseResponse.error(BaseResponseCodeEnum.CLI_ID_NOTNULL);
    	}
    	if (sharee.equalsIgnoreCase("admin")) {
    		sharee = getCurrentUser().getUserName();
        }
        int grade = taskService.getShare(sharee, gradeID);
        if (-1 == grade || -2 == grade) {
        	return BaseResponse.error(BaseResponseCodeEnum.DATA_NOT_FOUND);
    	}
    	HashMap<String, Integer> ret_info = new HashMap<>();
    	ret_info.put("grade", grade);
        return BaseResponse.success(ret_info);
    }

    @GetMapping(value="/testErrInfo")
    public BaseResponse<?> testErrInfo(@RequestParam(defaultValue = "") String err) {
        return BaseResponse.success(taskService.testErrInfo(err));
    }

    @GetMapping(value="/statsInfo")
    public BaseResponse<?> statsInfo(@RequestParam(defaultValue = "1") Integer step,
    		@RequestParam(defaultValue = "") String uuid,
    		@RequestParam(defaultValue = "") String query_id) {
    	if (step < 1 || step > 3) {
        	return BaseResponse.error(BaseResponseCodeEnum.CLI_PARAM_ILLEGAL);
    	}
        if (gatewayService.fromOlap(uuid)) {
            return BaseResponse.success(taskService.statsInfo(step, uuid, query_id));
        } else {
            return BaseResponse.success(gatewayService.statsInfo(step, uuid, query_id));
        }
    }

    @GetMapping(value="/getShares")
    public BaseResponse<?> getShares(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(defaultValue = "") String share_sql,
            @RequestParam(defaultValue = "") String sharer,
            @RequestParam(defaultValue = "") String engine,
            @RequestParam(defaultValue = "") String region) {
        return BaseResponse.success(taskService.getShares(pageNum, pageSize,
        		share_sql.toLowerCase(), sharer, engine.toLowerCase(),
        		region.toLowerCase(), getCurrentUser().getUserName()));
    }

    @ApiOperation("转换sql")
    @PostMapping(value="/transferBase64")
    public BaseResponse<?> transferBase64(@RequestParam @Valid String sql) {
        Map<String, String> statusObj = taskService.transSQLtoSpark(EncryptUtil.base64_decode(sql),
                getCurrentUser().getUserName());
        String base64SparkSql = EncryptUtil.base64_encode(statusObj.get("sparkSql"));
        statusObj.put("sparkSql", base64SparkSql);
        String base64TrinoSql = EncryptUtil.base64_encode(statusObj.get("trinoSql"));
        statusObj.put("trinoSql", base64TrinoSql);
        return BaseResponse.success(statusObj);
    }

    @ApiOperation("judge the qeury if form olap")
    @GetMapping(value="/fromOlap")
    public BaseResponse<?> fromOlap(@RequestParam @Valid String uuid) {
        return BaseResponse.success(gatewayService.fromOlap(uuid));
    }
}
