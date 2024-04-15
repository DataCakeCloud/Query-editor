package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.QueryHistory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface QueryHistoryService extends BaseService<QueryHistory> {

    /**
     * 获取保存查询列表
     *
     */
    PageInfo<QueryHistory> getQueryHistory(int pageNum, int pageSize,
            String query_id, String query_sql,
            String createBy, String engine, Integer status, Integer task_id,
            String info, String name, String userGroup);

    /**
     * 将param字段设置为JSONObject类型
     *
     */
    Map<String, Object> setParam(PageInfo<QueryHistory> queryHistoryList);
}
