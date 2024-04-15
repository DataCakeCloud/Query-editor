package com.ushareit.query.bean;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author wuyan
 * @date 2019/8/7
 **/
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CurrentUser {
    private int id;
    private String userId;
    private String userName;

    /**
     * 部门
     */
    @JSONField(name = "org")
    private String groupName;

    /**
     * 项目，梁又择使用
     */
    @JSONField(name = "group")
    private String group;

    private boolean isAdmin;
    private int tenantId;
    private String roles;
    private String tenantName;
    private String groupIds;
    private String groupUuid;
    private String defaultHiveDbName;
}

