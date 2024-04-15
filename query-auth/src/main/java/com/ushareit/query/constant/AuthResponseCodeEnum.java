package com.ushareit.query.constant;

import java.text.MessageFormat;

/**
 * 公共域 服务响应码
 *
 * @author swq
 * @date 2018/10/17
 */
public enum AuthResponseCodeEnum {
    /**
     * zeus
     */
    AUTH_ERROR(1016,"认证失败"),
    NO_LOGIN(1017,"未登录或登录过期，请先登录"),
    NO_AUTH(1018,"当前系统操作，没有权限"),

    FETCH_MENU_ERROR(1019,"菜单信息拉取失败"),

    FETCH_USER_ERROR(1020,"用户信息拉取失败"),
    ;

    private String message;
    private Integer code;

    AuthResponseCodeEnum(Integer code,String message) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }
    public Integer getCode() {
        return code;
    }

    @Override
    public String toString() {
        return MessageFormat.format("ResponseCode:{0},{1},{2}.", this.getCode(),this.name(), this.message);
    }

}
