package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.EngineAuth;

import java.util.HashMap;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface EngineAuthService extends BaseService<EngineAuth> {
    /**
     * 获取可选用户列表
     *
     */
    List<String> getUserAll();

    /**
     * 获取可选引擎列表
     *
     */
    HashMap<String, List<String>> getEngineAll();

    /**
     * 获取引擎权限列表
     *
     */
    PageInfo<EngineAuth> getEngineAuth(int pageNum, int pageSize, String info);
}
