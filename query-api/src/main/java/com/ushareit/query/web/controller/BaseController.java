package com.ushareit.query.web.controller;

import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.constant.AuthResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.exception.AuthException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 基础操作Controller
 *
 * @author wuyan
 * @date 2018/10/30
 **/
public abstract class BaseController {

    @Resource
    private HttpServletRequest request;

    public CurrentUser getCurrentUser() {
        CurrentUser currentUser = (CurrentUser) request.getAttribute(CommonConstant.CURRENT_LOGIN_USER);
        if (currentUser == null) {
            throw new AuthException(AuthResponseCodeEnum.NO_LOGIN);
        }
        return currentUser;
    }

    public String getUserInfo() {
        return request.getHeader(CommonConstant.CURRENT_LOGIN_USER);
    }
}
