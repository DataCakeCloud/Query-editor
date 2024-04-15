package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.MetaService;
import com.ushareit.query.service.ProbeService;
import com.ushareit.query.service.TaskService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;

/**
 * @author: tianxu
 * @create: 2022-06-13 17:00
 *
 */

@Api(tags = "数据探查")
@RestController
@RequestMapping("/probe")
public class ProbeController extends BaseBusinessController<QueryHistory> {

    @Autowired
    private ProbeService probeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MetaService metaService;

    @Override
    public BaseService getBaseService() {
        return (BaseService<QueryHistory>) probeService;
    }

    @ApiOperation("探查数据")
    @GetMapping(value = "/data")
    public BaseResponse<?> probeData(@RequestParam String uuid) {
        String user = getCurrentUser().getUserName();
        HashMap<String, Object> response = probeService.probe(uuid, user);

        if (response.get("code").equals(0) || response.get("code").equals(1) || response.get("code").equals(2)) {
            if (response.get("code").equals(2)) {
                JSONArray type = new JSONArray();
                JSONObject sample = probeService.getSample(uuid);
                taskService.probeAsyn(uuid, user, type, true, sample, getCurrentUser().getTenantName());
            }
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("探查扫描量")
    @PostMapping(value = "/scan")
    public BaseResponse<?> scanData(@RequestBody @Valid String params)  throws SQLException{
        String user = getCurrentUser().getUserName();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String name = getCurrentUser().getUserName();
        HashMap<String, Object> response = probeService.scan(map.get("uuid"), map.get("engine"), map.get("querySql").trim(), user);
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }
}
