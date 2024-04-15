package com.ushareit.query.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "olap")
public class OlapConfig {
    private Map<String, String> url;
    private Map<String, String> sslProperty;

    public Map<String, String> getUrl() {
        return url;
    }

    public Map<String, String> getSslProperty() {
        return sslProperty;
    }

    public void setUrl(Map<String, String> url) {
        this.url = url;
    }

    public void setSslProperty(Map<String, String> sslProperty) {
        this.sslProperty = sslProperty;
    }
}

