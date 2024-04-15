package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.bean.ShareGrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface SavedQueryService extends BaseService<SavedQuery> {

    /**
     * 获取保存查询列表
     *
     */
    PageInfo<SavedQuery> getSavedQuery(int pageNum, int pageSize,
            String title,
            String query_sql,
            String engine,
            Integer folderID,
            String region,
    		String info, String name, String userGroup);

    /**
     * 将param字段设置为JSONObject类型
     *
     */
    Map<String, Object> setParam(PageInfo<SavedQuery> savedQueryList);

    /**批量删除保存的查询
     *
     */
    String deleteBatch(String id);

    /**重复校验
     *
     */
    void preCheckCommon(SavedQuery savedQuery, String name);
    
    int addShareGrade(ShareGrade sg, String shareeEmail);
}
