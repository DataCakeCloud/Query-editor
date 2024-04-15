package com.ushareit.query.configuration;

import com.ushareit.query.web.interceptor.HttpRequestTraceFilter;
import org.hibernate.validator.HibernateValidator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.unit.DataSize;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import javax.servlet.MultipartConfigElement;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;


/**
 * 配置类
 *
 * @author wuyan
 * @date 2018/10/25
 **/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Bean
    public HttpRequestTraceFilter getTraceFilter() {
        return new HttpRequestTraceFilter();
    }

    @Bean
    public CorsFilter getCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    @Resource
    public FilterRegistrationBean<CorsFilter> corsFilter(CorsFilter filter) {
        final FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(10);
        return registrationBean;
    }

    @Configuration
    public class ValidatorConfiguration {
        @Bean
        public Validator validator() {
            ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                    .configure() // true-快速失败返回模式    false-普通模式
                    .addProperty("hibernate.validator.fail_fast", "true")
                    .buildValidatorFactory();
            return validatorFactory.getValidator();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilter() {
        final MultipartFilter multipartFilter = new MultipartFilter();
        final FilterRegistrationBean<MultipartFilter> registrationBean = new FilterRegistrationBean<>(multipartFilter);
        registrationBean.addInitParameter("multipartResolverBeanName", "commonsMultipartResolver");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<HttpRequestTraceFilter> traceFilter() {
//        final HttpRequestTraceFilter traceFilter = new HttpRequestTraceFilter();
        final HttpRequestTraceFilter traceFilter = getTraceFilter();
        final FilterRegistrationBean<HttpRequestTraceFilter> registrationBean =
                new FilterRegistrationBean<>(traceFilter);
        registrationBean.setFilter(traceFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return registrationBean;

    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(1024 * 1024 * 1024));
        return factory.createMultipartConfig();
    }

}
