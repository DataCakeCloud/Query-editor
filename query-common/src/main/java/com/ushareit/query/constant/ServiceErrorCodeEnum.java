package com.ushareit.query.constant;

import java.text.MessageFormat;

/**
 * 公共域 服务响应码
 *
 * @author swq
 * @date 2018/10/17
 */
public enum ServiceErrorCodeEnum {

    /**
     * 系统类响应
     */
    SYS_ERR("系统错误"),
    SYS_DB_CONN("数据库连接失败"),
    SYS_UNA("服务不可用"),
    SYS_DEGRADE("服务降级"),

    /**
     * 客户端类响应
     */
    CLI_PARAM_ILLEGAL("参数非法"),
    CLI_IDENTIFY_CODE_ILLEGAL("验证码非法"),
    CLI_ID_NOTNULL("ID不能为空"),
    CLI_DELETE_ILLEGAL("无效的删除对象，请继承DeleteEntity"),

    /**
     * 业务类型
     */
    DATA_NOT_FOUND("数据不存在"),
    NAME_IS_NOT_UNIQUE("数据库已经存在同名的记录"),
    USER_OR_PASSWORD_ERROR("用户名或密码错误"),

    SUCCESS("成功"),

    REQUEST_ILLEGAL("非法请求"),;

    private String message;

    ServiceErrorCodeEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return MessageFormat.format("ResponseCode:{0},{1}.", this.name(), this.message);
    }

}
