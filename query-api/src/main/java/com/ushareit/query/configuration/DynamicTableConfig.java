package com.ushareit.query.configuration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DynamicTableConfig {
	@Value("${spring.datasource.database}")
    private String database;
	@Value("${admin_tenant}")
	private String adminTenant;
	
	@Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 动态表名插件
        DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
        dynamicTableNameInnerInterceptor.setTableNameHandler(new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                log.info(String.format("multitenant getTenantName %s", InfTraceContextHolder.get().getTenantName()));
            	if (null == InfTraceContextHolder.get().getTenantName() ||
						adminTenant.equalsIgnoreCase(InfTraceContextHolder.get().getTenantName())) {
            		return tableName;
            	}
            	if (tableName.equals("account") || tableName.equals("engine") || tableName.equals("region")
            			|| tableName.equals("error_info")) {
            		return tableName;
            	}
            	if (!tableName.equals("classification") && !tableName.equals("data_api")
            			&& !tableName.equals("data_engineer") && !tableName.equals("engine_auth")
            			&& !tableName.equals("favortable") && !tableName.equals("query_history")
            			&& !tableName.equals("saved_query") && !tableName.equals("share_grade")
            			&& !tableName.equals("user") && !tableName.equals("chart")
            			&& !tableName.equals("classificationdash") && !tableName.equals("dash_chart")
            			&& !tableName.equals("dashboard") && !tableName.equals("favordashchart")
            			&& !tableName.equals("logview") && !tableName.equals("sharebi")
            			&& !tableName.equals("query_data") && !tableName.equals("query_result")
            			&& !tableName.equals("trans_sql")&& !tableName.equals("ai_chat")
						&& !tableName.equals("cron_query")) {
            		log.info(String.format("multitenant return table %s", tableName));
            		return tableName;
            	}
                log.info(String.format("multitenant new table %s", "`" + database + "_" + InfTraceContextHolder.get().getTenantName() + "`." + tableName));
                return "`" + database + "_" + InfTraceContextHolder.get().getTenantName() + "`." + tableName;
            }
        });
        interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        return interceptor;
    }
}

