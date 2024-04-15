package com.ushareit.query.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ushareit.query.constant.AuthResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.exception.AuthException;
import com.ushareit.query.utils.WebUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
//import com.ushareit.interceptor.model.RemoteResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 过滤器记录请求日志
 *
 * @author wuyan
 * @date 2018/10/26
 **/
@Slf4j
@Component
public class AuthAccessFilter extends AbstractAuthFilter {
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        boolean match = WebUtils.methodIsMatch(request, CommonConstant.IGNORE_METHOD)
                || WebUtils.pathIsMatch(request, CommonConstant.IGNORE_INTERCEPT_PATHS)
                || WebUtils.pathIsMatch(request, CommonConstant.OIDC_INTERCEPT_PATHS)
                || WebUtils.headerIsMatch(request, CommonConstant.OIDC_HEADERS);

        if (match) {
            chain.doFilter(request, response);
            return;
        }

        /*String authentication = request.getHeader(CommonConstant.AUTHENTICATION_HEADER);
        HttpHeaders headers = new HttpHeaders();
        headers.add(CommonConstant.AUTHENTICATION_HEADER, authentication);
        Map<String, String> info = Maps.newHashMap();
        info.put("uri", requestUri);
        info.put("systemCode", systemCode);
        RemoteResult remoteResult = this.rpcComponent.invokeGet(TEMPLATE, accessUrl, headers, info, RemoteResult.class);
        if (remoteResult == null || !remoteResult.isSuccess()) {
            request.setAttribute(CommonConstant.AUTH_EXCEPTION, AuthResponseCodeEnum.NO_AUTH);
            throw new AuthException(AuthResponseCodeEnum.NO_AUTH);
        }*/
        chain.doFilter(request, response);
    }

}
