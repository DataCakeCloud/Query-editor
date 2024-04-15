package com.ushareit.query.service;

import com.ushareit.query.bean.User;
import com.ushareit.query.bean.Region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface UserService extends BaseService<User> {
    /**
     * 获取用户group
     *
     * @param name
     */
//    String getUserGroup(String name);

    /**
     * 获取用户engine
     *
     * @param name
     */
    List<String> getUserEngine(String name, String region);

    /**
     * 获取所有region
     *
     */
    List<HashMap<String, String>> getRegions(String userInfo, int tenantId);
}
