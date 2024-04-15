package com.ushareit.query.web.controller;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.CronQuery;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.CronQueryService;
import io.swagger.annotations.Api;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;

@Api(tags = "定时查询相关")
@RestController
@Setter
@RequestMapping("/cronQuery")
public class CronQueryController extends BaseBusinessController<CronQuery> {
    @Autowired
    private CronQueryService cronQueryService;

    @Override
    public BaseService<CronQuery> getBaseService() {
        return cronQueryService;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody @Valid String params, @RequestHeader("Authentication") String token) throws IOException {
        CurrentUser curUser = getCurrentUser();
        return cronQueryService.create(params, token, curUser);
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody @Valid String params, @RequestHeader("datacake_token") String token) throws IOException, SQLException, ParseException {
        CurrentUser curUser = getCurrentUser();
        return cronQueryService.execute(params, token, curUser, getUserInfo());
    }

    @PostMapping("/history")
    public Map<String, Object> history(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "50") Integer pageSize,
                                       @RequestBody @Valid String params) throws IOException {
        if (params.contains("%5B")) {
            params = params.replace("%5B", "[");
        }
        if (params.contains("%5D")) {
            params = params.replace("%5D", "]");
        }
        if (params.contains("%7B")) {
            params = params.replace("%7B", "{");
        }
        if (params.contains("%7D")) {
            params = params.replace("%7D", "}");
        }
        if (params.contains("%5C")) {
            params = params.replace("%5C", "\\");
        }
        CurrentUser curUser = getCurrentUser();
        PageInfo<CronQuery> queryHistoryList = cronQueryService.history(pageNum, pageSize, params, curUser, getUserInfo());
        return cronQueryService.setParam(queryHistoryList);
    }

    @PostMapping("/changeStatus")
    public Map<String, Object> changeStatus(@RequestBody @Valid String params, @RequestHeader("Authentication") String token) throws IOException, InterruptedException {
        CurrentUser curUser = getCurrentUser();
        return cronQueryService.changeStatus(params, token, curUser);
    }
}
