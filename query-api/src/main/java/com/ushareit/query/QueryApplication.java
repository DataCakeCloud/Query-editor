package com.ushareit.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

@ServletComponentScan
@MapperScan(basePackages = {"com.ushareit.query.mapper"})
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.ushareit.interceptor", "com.ushareit.query"})
public class QueryApplication {
    public static void main(String[] args) {SpringApplication.run(QueryApplication.class, args);}
}