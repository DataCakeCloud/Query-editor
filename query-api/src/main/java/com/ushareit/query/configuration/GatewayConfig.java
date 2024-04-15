package com.ushareit.query.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Data
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfig {
    private Map<String, String> spark_cluster_tags;
    private Map<String, String> trino_cluster_tags;
    private Map<String, String> hive_cluster_tags;
}
