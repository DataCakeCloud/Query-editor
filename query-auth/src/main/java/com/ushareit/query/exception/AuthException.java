package com.ushareit.query.exception;

import com.ushareit.query.constant.AuthResponseCodeEnum;

/**
 * @author wuyan
 * @date 2019/8/7
 **/
public class AuthException extends RuntimeException {
    private static final long serialVersionUID = -7828337362960040358L;

    private String code;

    private Object data;

    public AuthException() {
    }
    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AuthException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public AuthException(AuthResponseCodeEnum kafkaOpsResponseCodeEnum) {
        this(kafkaOpsResponseCodeEnum.name(), kafkaOpsResponseCodeEnum.getMessage());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }


}