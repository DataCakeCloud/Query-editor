package com.ushareit.query.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.constant.AuthResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.exception.AuthException;
import com.ushareit.query.service.third.OIDCIdentityService;
import com.ushareit.query.utils.WebUtils;
//import com.ushareit.interceptor.model.RemoteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 过滤器记录请求日志
 *
 * @author wuyan
 * @date 2018/10/26
 **/
@Slf4j
@Component
public class AuthIdentityFilter extends AbstractAuthFilter {
    //@Value("${zeus.login.intercept.active}")
    //private boolean active;

    @Resource
    private OIDCIdentityService oidcIdentityService;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
        /*if (!this.active) {
            request.setAttribute(CommonConstant.CURRENT_LOGIN_USER,
                    new CurrentUser(1, "admin", "admin", "cbs", "DW",
                    		true, 1, "root", "bdp", "1"));
            chain.doFilter(request, response);
            return;
        }*/
        boolean match = WebUtils.requestAnyIsMatch(request, CommonConstant.IGNORE_METHOD,
                CommonConstant.IGNORE_CONTENT, CommonConstant.IGNORE_INTERCEPT_PATHS);

        if (match) {
            chain.doFilter(request, response);
            return;
        }
        /*String authentication = request.getHeader(CommonConstant.AUTHENTICATION_HEADER);
        HttpHeaders headers = new HttpHeaders();
        headers.add(CommonConstant.AUTHENTICATION_HEADER, authentication);
        RemoteResult remoteResult = this.rpcComponent.invokeGet(TEMPLATE, identityUrl, headers, RemoteResult.class);
        if (remoteResult == null || !remoteResult.isSuccess()) {
            log.error("-----getContextPath:" + request.getContextPath());
            log.error("-----getRequestURI:" + request.getRequestURI());
            log.error("-----authentication:" + authentication);
            log.error("-----identityUrl:" + identityUrl);
            log.error("-----getReturnCode:" + remoteResult.getReturnCode());
            log.error("-----getMessage:" + remoteResult.getMessage());
            log.error("-----getData:" + remoteResult.getData());
            request.setAttribute(CommonConstant.AUTH_EXCEPTION, AuthResponseCodeEnum.NO_LOGIN);
            throw new AuthException(AuthResponseCodeEnum.NO_LOGIN);
        }

        if (remoteResult.getData() != null) {
            CurrentUser currentUser =
                    JSONObject.parseObject(JSONObject.toJSONString(remoteResult.getData()), CurrentUser.class);
            String userName =
                    StringUtils.isBlank(currentUser.getUserName()) ? currentUser.getUserId()
                            : currentUser.getUserName();
            currentUser.setUserName(userName);
            request.setAttribute(CommonConstant.CURRENT_LOGIN_USER, currentUser);
        }*/
    	String userInfo = request.getHeader(CommonConstant.CURRENT_LOGIN_USER);
    	if (null == userInfo) {
            request.setAttribute(CommonConstant.AUTH_EXCEPTION, AuthResponseCodeEnum.NO_LOGIN);
            throw new AuthException(AuthResponseCodeEnum.NO_LOGIN);
    	}
        String groupId = request.getHeader("Groupid");
        String currentGroup = request.getHeader("Currentgroup");
        String uuidGroup = request.getHeader("Uuid");
        if (null == groupId || null == currentGroup || null == uuidGroup) {
            request.setAttribute(CommonConstant.AUTH_EXCEPTION, AuthResponseCodeEnum.NO_LOGIN);
            throw new AuthException(AuthResponseCodeEnum.NO_LOGIN);
        }
        String defaultHiveDbName = "";
        /*Map<String, Object> content = JSON.parseObject(userInfo, Map.class);
        List listObjects = JSON.parseObject(content.get("userGroup").toString(), List.class);
        for (int i = 0; i < listObjects.size(); ++i) {
            Map<String, Object> groupInfo = JSON.parseObject(listObjects.get(i).toString(), Map.class);
            if (uuidGroup.equals(groupInfo.get("uuid"))) {
                defaultHiveDbName = groupInfo.get("defaultHiveDbName").toString();
                break;
            }
        }*/

    	CurrentUser currentUser =
                JSONObject.parseObject(userInfo, CurrentUser.class);
        String userName =
                StringUtils.isBlank(currentUser.getUserName()) ? currentUser.getUserId()
                        : currentUser.getUserName();
        currentUser.setUserName(userName);
        currentUser.setGroupIds(groupId);
        currentUser.setGroupName(currentGroup);
        currentUser.setGroup(currentGroup);
        currentUser.setGroupUuid(uuidGroup);
        currentUser.setDefaultHiveDbName(defaultHiveDbName);
        log.error("currentUser: {}, currentgroupId: {}, currentGroup: {}, uuidGroup: {}, defaultHiveDbName: {}",
                currentUser, groupId, currentGroup, uuidGroup, defaultHiveDbName);
        request.setAttribute(CommonConstant.CURRENT_LOGIN_USER, currentUser);
        chain.doFilter(request, response);
    }

    String getToken(HttpServletRequest request) {
        log.info("oidc check, uri: {}, host:{}", request.getRequestURI(), WebUtils.getClientIpAddress(request));
        // do openapi identity
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isBlank(authorization) || !authorization.contains(StringUtils.SPACE)) {
            log.error("request header: Authorization is empty or invalid");
            return null;
        }

        return authorization.split(StringUtils.SPACE)[1];
    }

}
