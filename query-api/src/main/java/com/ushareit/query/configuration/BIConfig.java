package com.ushareit.query.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "bi")
public class BIConfig {
    private Map<String, String> url;
    private Map<String, String> username;
    private Map<String, String> password;

    public Map<String, String> getUrl() {
        return url;
    }

    public void setUrl(Map<String, String> url) {
        this.url = url;
    }

    public Map<String, String> getUsername() {
        return username;
    }

    public void setUsername(Map<String, String> username) {
        this.username = username;
    }

    public Map<String, String> getPassword() {
        return password;
    }

    public void setPassword(Map<String, String> password) {
        this.password = password;
    }
}

