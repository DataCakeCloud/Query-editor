package com.ushareit.query.configuration;

import javax.annotation.Resource;

import com.ushareit.query.filter.AuthIdentityFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ushareit.query.filter.AuthAccessFilter;
import com.ushareit.query.filter.AuthFilterExceptionFilter;
import com.ushareit.query.filter.AbstractAuthFilter;

/**
 * 配置类
 *
 * @author wuyan
 * @date 2018/10/25
 **/
@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {


    @Resource
    AuthAccessFilter authAccessFilter;
    @Resource
    AuthIdentityFilter authIdentityFilter;
    @Resource
    AuthFilterExceptionFilter authFilterExceptionFilter;

    @Bean
    public FilterRegistrationBean<AuthFilterExceptionFilter> authFilter() {
        final FilterRegistrationBean<AuthFilterExceptionFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authFilterExceptionFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AuthAccessFilter> accessFilter() {
        final FilterRegistrationBean<AuthAccessFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authAccessFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 200);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AbstractAuthFilter> identityFilter() {
        final FilterRegistrationBean<AbstractAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authIdentityFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registrationBean;
    }

}
