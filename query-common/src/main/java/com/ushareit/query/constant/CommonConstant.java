package com.ushareit.query.constant;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

/**
 * @author wuyan
 * @date 2018/11/14
 */
public class CommonConstant {

    /**
     * 通过OPENAPI认证的请求path
     */
    public static final String[] OIDC_INTERCEPT_PATHS = {"/oidc/**"};

    /**
     * 通过 OpenApi 认证的请求，携带的 header
     */
    public static final Header[] OIDC_HEADERS = {new BasicHeader("OidcKey", "OpenApi")};

    /**
     * 忽略拦截或过滤的请求path
     */
    public static final String[] IGNORE_INTERCEPT_PATHS = {"/", "/csrf","/webjars/**", "/static/**", "/error/**", "/logout", "/code", "/login",
            "/inf-druid/**", "/druid/**", "/index", "/index.html", "/version", "/favicon.ico",
            "/swagger-resources/**", "/health","/swagger-ui.html","/v2/api-docs", "/**/remote","/artifactversion/download*","/editor.worker.js","/task/statushook"
    };

    /**
     * DDL
     * 不支持load
     */
    public static final String[] DDL = {"create", "alter", "desc", "describe", "msck", "drop", "show"};


    /**
     * 忽略的CONTENT_TYPE
     **/
    public static final String[] IGNORE_CONTENT = {};

    /**
     * 忽略拦截或过滤的请求方法
     **/
    public static final String[] IGNORE_METHOD = {"HEAD", "OPTIONS", "TRACE", "CONNECT"};

    public static final String CURRENT_LOGIN_USER = "current_login_user";

    public static final String AUTHENTICATION_HEADER = "Authentication";
    public static final String DINGTALK_TOKEN_HEADER = "Token";
    public static final String DINGTALK_ACCESS_PATH = "/access";
    public static final String DINGTALK_ACCESS_METHOD = "POST";

    public static final String AUTH_EXCEPTION = "auth_exception";
    public static final Integer DOWNLOAD_THREADS = 5;

    public static final String RENDER_SQL = "/task/renderSql";
    public static final String SQL_SET = "set ";
    public static final String TRINO_SESSION = "SESSION ";

    /**
     * 管理员
     */
    public static final Integer ADMIN = 2;
}
