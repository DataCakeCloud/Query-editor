package com.ushareit.query.exception;

import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.web.vo.BaseResponse;

/**
 * @author swq
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = -7828337362960040358L;

    private Integer code = 500;

    private String codeStr;

    private Object data;

    public ServiceException(String codeStr, String message) {
        super(message);
        this.codeStr = codeStr;
    }

    public ServiceException(String codeStr, String message, Object data) {
        super(message);
        this.codeStr = codeStr;
        this.data = data;
    }

    public ServiceException(String message, Throwable throwable) {
        super(message + ", " + throwable.getMessage(), throwable);
    }

    public ServiceException(BaseResponseCodeEnum serviceErrorCodeEnum) {
        this(serviceErrorCodeEnum.name(), serviceErrorCodeEnum.getMessage());
    }

    public ServiceException(BaseResponseCodeEnum serviceErrorCodeEnum, Object o) {
        this(serviceErrorCodeEnum.name(), serviceErrorCodeEnum.getMessage(), o);
    }

    public ServiceException(BaseResponse baseResponse) {
        this(baseResponse.getCodeStr(), baseResponse.getMessage(), baseResponse.getData());
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getCodeStr() {
        return codeStr;
    }

    public void setCodeStr(String codeStr) {
        this.codeStr = codeStr;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
