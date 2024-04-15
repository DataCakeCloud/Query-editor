package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.DataAPI;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.DataAPIService;
import com.ushareit.query.service.TaskService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Api(tags = "保存的API")
@RestController
@RequestMapping("/dataService")
public class DataAPIController extends BaseBusinessController<DataAPI> {
    @Autowired
    private DataAPIService dataAPIService;

    @Autowired
    private TaskService taskService;

    @Override
    public BaseService<DataAPI> getBaseService() {
        return dataAPIService;
    }

    @ApiOperation(value = "保存的数据API列表")
    @GetMapping("/api/all")
    public BaseResponse getDataAPI(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "50") Integer pageSize,
                                   @RequestParam(defaultValue = "",required=false)String title,
                                   @RequestParam(defaultValue = "",required=false)String path,
                                   @RequestParam(defaultValue = "",required=false)String querySql,
                                   @RequestParam(defaultValue = "",required=false)String engineZh,
                                   @RequestParam(defaultValue = "",required=false)String param,
                                   @RequestParam(defaultValue = "",required=false)String createBy,
                                   @RequestParam(defaultValue = "9",required=false)Integer status,
                                   @RequestParam(defaultValue = "",required=false)String region) {
        if (path.contains("%7B")) {
            path = path.replace("%7B", "{");
        }
        if (path.contains("%7D")) {
            path = path.replace("%7D", "}");
        }
        if (querySql.contains("%7B")) {
            querySql = querySql.replace("%7B", "{");
        }
        if (querySql.contains("%7D")) {
            querySql = querySql.replace("%7D", "}");
        }
        String user = getCurrentUser().getUserName();
        PageInfo<DataAPI> apiPage = dataAPIService.getDataAPI(pageNum, pageSize, title,
        		path, querySql, engineZh, param,
        		createBy, status, user, region);
        Map<String, Object> apiPageInfo = dataAPIService.setCountInfo(apiPage);
        return BaseResponse.success(apiPageInfo);
    }

    @ApiOperation(value = "创建数据API")
    @PostMapping("/api/add")
    public BaseResponse addDataApi(@RequestBody @Valid String params) {
        String user = getCurrentUser().getUserName();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String title = map.get("title");
        String engine = map.get("engine");
        String querySql = map.get("querySql");
        String uuid = map.get("uuid");
        String param = map.get("param");
        String region = map.get("region");
        String catalog = map.get("catalog");
        if (null == region) {
        	region = "";
        }
        if (null == catalog) {
        	catalog = "";
        }
        return BaseResponse.success(dataAPIService.addDataApi(title, engine, querySql,
        		uuid, param, user, region, catalog));
    }

    @ApiOperation(value = "更新数据API")
    @PutMapping("/api/update")
    public BaseResponse updateDataApi(@RequestBody @Valid String params) {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> map = JSON.parseObject(params, HashMap.class);
        Integer id = Integer.parseInt(map.get("id").toString());
        String title = map.get("title").toString();
        String engine = map.get("engine").toString();
        String querySql = map.get("querySql").toString();
        String uuid = map.get("uuid").toString();
        String param = map.get("param").toString();
        String region = "";
        if (null != map.get("region")) {
        	region = map.get("region").toString();
        }
        String catalog = "";
        if (null != map.get("catalog")) {
        	catalog = map.get("catalog").toString();
        }
        return BaseResponse.success(dataAPIService.updateDataApi(id, title, engine, querySql,
        		uuid, param, user, region, catalog));
    }

    @ApiOperation(value = "API上线/下线")
    @PutMapping("/api/status")
    public BaseResponse setStatus(@RequestBody @Valid String params) {
        HashMap<String, Integer > map = JSON.parseObject(params, HashMap.class);
        Integer id = map.get("id");
        Integer status = map.get("status");
        return BaseResponse.success(dataAPIService.setAPIStatus(id, status));
    }

    @ApiOperation(value = "批量删除保存的API")
    @DeleteMapping("/api/deleteBatch")
    public BaseResponse deleteBatch(@RequestParam String id) {
        return BaseResponse.success(dataAPIService.deleteBatch(id));
    }

    @ApiOperation("API名称重复校验")
    @GetMapping(value = "/api/nameCheck")
    public BaseResponse<?> nameCheck(@RequestParam("title") String title) {
        String user = getCurrentUser().getUserName();
        ArrayList<String> titleList = dataAPIService.getTitleList(user);
        String isExisted = dataAPIService.nameCheck(titleList, title);
        return BaseResponse.success(isExisted);
    }

    @ApiOperation("获取API数据")
    @GetMapping(value = "/api/data")
    public BaseResponse<?> getData(@RequestParam Map<String, Object> map) throws ParseException, SQLException {
//        String user = getCurrentUser().getUserName();

        Integer id = Integer.parseInt(map.get("id").toString());

        DataAPI savedApi = dataAPIService.getDataAPIByID(id);
        String engine = savedApi.getEngine();
        String querySql = savedApi.getQuerySql();
        String param = savedApi.getParam();
        Integer status = savedApi.getStatus();
        String user = savedApi.getCreateBy();
        String region = savedApi.getRegion();
        String catalog = savedApi.getCatalog();
        Integer isAsync = taskService.getMysqlAsync(engine);
        HashMap<String, Object> response = new HashMap<>();
        CurrentUser curUser = getCurrentUser();
        String strGIds = curUser.getGroupIds();
        String groupId = null;
        if (null != strGIds) {
            String groupIds[] = curUser.getGroupIds().split(",");
        	groupId = groupIds[0];
        }
        int tenantId = curUser.getTenantId();
        String tenantName = curUser.getTenantName();

        if (status == 0) {
            String uuid = dataAPIService.getUuid();
            String sql = dataAPIService.getQuerySql(map, querySql, param);
            if (engine.startsWith("mysql") && isAsync == 0) {
                taskService.executeMysqlAsyn(uuid, engine, sql.trim(),
                    null, null, user, region, catalog, groupId, tenantId, tenantName);
                response = taskService.executeMysqlSample(uuid, engine, sql.trim(), user);
            } else {
                response = taskService.execute(uuid, engine, sql.trim(), 1, 1,
                    user, region, catalog, "", groupId, tenantId, tenantName, null);
            }

            dataAPIService.updateApiIdToHistory(id, uuid, user);
            if (response.get("code").equals(0) || response.get("code").equals(2)){
                return BaseResponse.success(response.get("data"));
            } else {
                return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
            }
        } else {
            return BaseResponse.error("500", "此API已下线，无法进行调用");
        }
    }
}
