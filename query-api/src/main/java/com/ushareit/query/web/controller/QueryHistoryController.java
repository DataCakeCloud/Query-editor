package com.ushareit.query.web.controller;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.service.QueryHistoryService;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Api(tags = "查询历史")
@RestController
@RequestMapping("/queryHistory")
public class QueryHistoryController extends BaseBusinessController<QueryHistory> {

    @Autowired
    private QueryHistoryService queryHistoryService;

    @Override
    public BaseService<QueryHistory> getBaseService() {
        return queryHistoryService;
    }

    @ApiOperation(value = "查询历史列表")
    @GetMapping("/all")
    public BaseResponse getQueryHistory(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "50") Integer pageSize,
                                      @RequestParam(defaultValue = "-1") Integer query_id,
                                      @RequestParam(defaultValue = "") String query_sql,
                                      @RequestParam(defaultValue = "") String createBy,
                                      @RequestParam(defaultValue = "") String engine,
                                      @RequestParam(defaultValue = "-1") Integer status,
                                      @RequestParam(defaultValue = "") Integer task_id,
                                      @RequestParam String info) {
        if (info.contains("%5B")) {
            info = info.replace("%5B", "[");
        }
        if (info.contains("%5D")) {
            info = info.replace("%5D", "]");
        }
        if (info.contains("%7B")) {
            info = info.replace("%7B", "{");
        }
        if (info.contains("%7D")) {
            info = info.replace("%7D", "}");
        }
        if (info.contains("%5C")) {
            info = info.replace("%5C", "\\");
        }
        String name = getCurrentUser().getUserName();
        String qid = "";
        if (-1 != query_id) {
            qid = String.valueOf(query_id);
        }
        PageInfo<QueryHistory> queryHistoryList = queryHistoryService.getQueryHistory(pageNum, pageSize,
        		qid, query_sql, createBy, engine, status, task_id,
        		info, name, getCurrentUser().getGroupUuid());
        Map<String, Object> queryHistoryPageInfo = queryHistoryService.setParam(queryHistoryList);
        return BaseResponse.success(queryHistoryPageInfo);
    }
}
