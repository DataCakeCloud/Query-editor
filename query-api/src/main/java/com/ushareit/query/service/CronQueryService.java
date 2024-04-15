package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.CronQuery;
import com.ushareit.query.bean.CurrentUser;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;

public interface CronQueryService extends BaseService<CronQuery> {
    Map<String, Object> create(String params, String token, CurrentUser curUser) throws IOException;

    Map<String, Object> execute(String params, String token, CurrentUser curUser, String userInfo) throws IOException, SQLException, ParseException;

    PageInfo<CronQuery> history(Integer pageNum, Integer pageSize, String params, CurrentUser curUser, String userInfo) throws IOException;

    Map<String, Object> setParam(PageInfo<CronQuery> queryHistoryList);

    Map<String, Object> changeStatus(String params, String token, CurrentUser curUser) throws IOException, InterruptedException;
}
