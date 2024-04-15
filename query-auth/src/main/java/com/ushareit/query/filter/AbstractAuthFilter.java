package com.ushareit.query.filter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

//import com.ushareit.interceptor.component.RpcComponent;
//import com.ushareit.interceptor.rest.RestTemplateFactory;

/**
 * 过滤器记录请求日志
 *
 * @author wuyan
 * @date 2018/10/26
 **/
public abstract class AbstractAuthFilter extends OncePerRequestFilter {

    //@Value("${system.code}")
    //public String systemCode;

    //@Resource
    //public RpcComponent rpcComponent;

    //public static RestTemplate TEMPLATE;

    //@PostConstruct
    //public void init() {
    //    TEMPLATE = RestTemplateFactory.getRestTemplate(systemCode);
    //}

}
