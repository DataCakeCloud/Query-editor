package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.bean.User;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.mapper.UserMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.QueryHistoryService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Slf4j
@Service
@Setter
public class QueryHistoryServiceImpl extends AbstractBaseServiceImpl<QueryHistory> implements QueryHistoryService {

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public CrudMapper<QueryHistory> getBaseMapper() { return queryHistoryMapper; }

    @Override
    public PageInfo<QueryHistory> getQueryHistory(int pageNum, int pageSize,
            String query_id, String query_sql,
            String createBy, String engine, Integer status, Integer task_id,
            String info, String name, String userGroup) {
        log.info(String.format("%s start to get query history", name));
    	boolean isDetail = false;
        if (null != query_id && query_id.trim().length() > 0 && !query_id.trim().equals("-1") ||
        		null != query_sql && query_sql.trim().length() > 0 ||
        		null != engine && engine.trim().length() > 0 ||
                null != createBy && createBy.trim().length() > 0 ||
        		null != status && -1 != status ||
                null != task_id) {
        	isDetail = true;
        }
        User user = userMapper.selectByName(name);
        Integer isAdmin = 0;
        if (user != null) {
            isAdmin = user.getIsAdmin();
        }
        if (isAdmin == 1) {
//            System.out.println("User [" + name + "] is admin!");
            PageHelper.startPage(pageNum, pageSize);
            List<QueryHistory> pageRecord = null;
            if (isDetail) {
            	pageRecord = queryHistoryMapper.listByAdminDetails(info, query_id,
            			query_sql, createBy, engine, status, userGroup, task_id);
            } else {
            	pageRecord = queryHistoryMapper.listForAdmin(info, userGroup);
            }
            return new PageInfo<>(pageRecord);
        } else {
//            System.out.println("User [" + name + "] is not admin!");
            PageHelper.startPage(pageNum, pageSize);
            List<QueryHistory> pageRecord = null;
            if (isDetail) {
            	pageRecord = queryHistoryMapper.listByDetails(info, query_id,
            			query_sql, engine, status, task_id, userGroup, name);
            } else {
            	pageRecord = queryHistoryMapper.listByInfo(info, name, userGroup);
            }
            return new PageInfo<>(pageRecord);
        }
    }

    @Override
    public Map<String, Object> setParam(PageInfo<QueryHistory> queryHistoryList) {
        ArrayList<Object> queryHistory = new ArrayList<>();
        List<QueryHistory> pageList = queryHistoryList.getList();
        Map<String, Object> pageObject = JSON.parseObject(JSON.toJSONString(queryHistoryList));

        for (int i = 0; i < pageList.size(); i++) {
            Map<String, Object > data = JSON.parseObject(JSON.toJSONString(pageList.get(i)));
            if (data.get("param") != null) {
                data.put("param", JSONObject.parseObject(data.get("param").toString()));
            }
            queryHistory.add(data);
        }
        pageObject.put("list", queryHistory);
        return pageObject;
    }
}
