package com.ushareit.query.web.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.constant.DsTaskConstant;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.utils.RequestWrapper;
import com.ushareit.query.utils.WebUtils;
//import com.ushareit.query.utils.ZeusUtil;
import com.ushareit.query.web.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tk.mybatis.mapper.util.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 过滤器记录请求日志
 *
 * @author wuyan
 * @date 2018/10/26
 **/
@Slf4j
@Component
public class HttpRequestTraceFilter extends OncePerRequestFilter {
    //@Value("${spring.profiles.active}")
    //private String active;

    //@Autowired
    //private ZeusUtil zeusUtil;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {

        boolean match = WebUtils.requestAnyIsMatch(request, CommonConstant.IGNORE_METHOD,
                CommonConstant.IGNORE_CONTENT, CommonConstant.IGNORE_INTERCEPT_PATHS);

        if (match) {
            chain.doFilter(request, response);
            return;
        }

        String traceId = request.getHeader(DsTaskConstant.LOG_TRACE_ID);
        if (null == traceId || traceId.isEmpty()) {
            traceId = UuidUtil.getUuid32();
        }
        // put slf4j mdc
        MDC.put(DsTaskConstant.LOG_TRACE_ID, traceId);
        //InfTraceContextHolder.get().setEnv(this.active);

        String requestBody = "";
        ServletRequest servletRequest = request;
        match = WebUtils.contentIsMatch(request, DsTaskConstant.IGNORE_CONTENT);
        if (!match) {
            servletRequest = new RequestWrapper(request);
            requestBody = new String(((RequestWrapper) servletRequest).getBody());
        }

        //最终参数列表，包含请求路径参数和body中的参数
        Map<String, String> resultMap = getQueryMap(request, requestBody);
        InfTraceContextHolder.get().getMap().putAll(resultMap);

        CurrentUser currentUser = (CurrentUser) request.getAttribute(CommonConstant.CURRENT_LOGIN_USER);
        InfTraceContextHolder.get().setUserName(currentUser.getUserName());
        InfTraceContextHolder.get().setNewCode(currentUser.getGroupName());
        InfTraceContextHolder.get().setTenantId(currentUser.getTenantId());
        InfTraceContextHolder.get().setTenantName(currentUser.getTenantName());

        String requestPath =
                StringUtils.isBlank(request.getServletPath()) ? request.getRequestURI() : request.getServletPath();
        String queryPath = request.getQueryString();
        requestPath = requestPath + (queryPath == null ? "" : "?" + queryPath);

        /*if (!checkAuth(request, currentUser.getGroupName())) {
            request.setAttribute(CommonConstant.AUTH_EXCEPTION, BaseResponseCodeEnum.NO_RIGHT);
        }*/
        String authentication = request.getHeader(CommonConstant.AUTHENTICATION_HEADER);

        InfTraceContextHolder.get().setTraceId(traceId).setRequestPath(requestPath)
                .setRequestBody(requestBody)
                .setSessionId(request.getSession().getId())
                .setAuthentication(authentication);

        chain.doFilter(servletRequest, response);
    }

    /*private boolean checkAuth(HttpServletRequest request, String tenancyCode) {
        Boolean admin = zeusUtil.isAdmin(request);
        if (admin) {
            InfTraceContextHolder.get().setAdmin(true);
            return true;
        }
        InfTraceContextHolder.get().setAdmin(false);
        String oldCode = InfTraceContextHolder.get().getOldCode();
        if (oldCode == null) {
            return true;
        }
        return checkTenancyCode(oldCode, tenancyCode);
    }*/

    private boolean checkTenancyCode(String oldCode, String newCode) {
        //修改信息的人的组与之前对象中的组不一致，
        String[] now = newCode.split(",");
        for (String group : now) {
            if (oldCode.contains(group)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> getQueryMap(HttpServletRequest request, String requestBody) {
        if (!StringUtil.isEmpty(requestBody)) {
            return getJsonQueryMap(requestBody);
        }
        switch (request.getMethod()) {
            case DsTaskConstant.POST_METHOD:
                return getParameterQueryMap(request);
            default:
                return getUrlQueryMap(request);
        }
    }

    private Map<String, String> getJsonQueryMap(String requestBody) {
        return (Map<String, String>) JSONObject.parseObject(requestBody, Map.class);
    }

    private Map<String, String> getUrlQueryMap(HttpServletRequest request) {
        String urlQueryString = request.getQueryString();
        Map<String, String> queryMap = new HashMap<>(8);
        String[] arrSplit;
        if (urlQueryString == null) {
            return queryMap;
        } else {
            //每个键值为一组
            arrSplit = urlQueryString.split("[&]");
            for (String strSplit : arrSplit) {
                String[] arrSplitEqual = strSplit.split("[=]");
                //解析出键值
                if (arrSplitEqual.length > 1) {
                    queryMap.put(arrSplitEqual[0], arrSplitEqual[1]);
                } else {
                    if (!"".equals(arrSplitEqual[0])) {
                        queryMap.put(arrSplitEqual[0], "");
                    }
                }
            }
        }
        return queryMap;
    }

    private Map<String, String> getParameterQueryMap(HttpServletRequest request) {
        Enumeration enumeration = request.getParameterNames();
        Map<String, String> queryMap = new HashMap<>(8);
        while (enumeration.hasMoreElements()) {
            String paramName = (String) enumeration.nextElement();
            String[] values = request.getParameterValues(paramName);
            if (values.length != 0) {
                queryMap.put(paramName, values[0]);
            } else {
                queryMap.put(paramName, "");
            }
        }
        return queryMap;
    }
}
