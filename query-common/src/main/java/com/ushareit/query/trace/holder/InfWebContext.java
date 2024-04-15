package com.ushareit.query.trace.holder;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wuyan
 * @date 2020/05/11
 */
@Data
@Accessors(chain = true)
public class InfWebContext {
    private String traceId;

    private Integer applicationId;
    private String userName;
    private String oldCode;
    private String newCode;
    private long startTime;
    private String clientIp;
    private String requestPath;
    private String requestBody;
    private String env;
    private Boolean admin;
    private String sessionId;
    private String authentication;
    private int tenantId;
    private String tenantName;

    private Map<String, Object> map = new HashMap<>();

    public String getEnv(){
        return env.split(",")[1].trim();
    }
}
