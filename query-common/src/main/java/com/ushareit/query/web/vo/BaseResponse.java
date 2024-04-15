package com.ushareit.query.web.vo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author swq
 * @date 2018/3/16
 */
@Data
public class BaseResponse<T> {
    private Integer code = 0;
    /**
     * 错误码
     */
    private String codeStr = BaseResponseCodeEnum.SUCCESS.name();
    /**
     * 消息
     */
    private String message;
    /**
     * 响应内容
     */
    private T data;
    /**
     * 错误类型
     */
    private String errorType;
    /**
     * 错误中文描述
     */
    private String errorZh;


    public BaseResponse() {
    }

    private BaseResponse(T data) {
        this.data = data;
    }

    private BaseResponse(String codeStr, String message, T data) {
        if (!"0".equals(codeStr) && !BaseResponseCodeEnum.SUCCESS.name().equals(codeStr)){
            this.code = 500;
            this.codeStr = BaseResponseCodeEnum.SYS_ERR.name();
        }
        this.message = message;
        this.data = data;
    }

    private BaseResponse(String codeStr, String message, T data, String errorType, String errorZh) {
        if (!"0".equals(codeStr) && !BaseResponseCodeEnum.SUCCESS.name().equals(codeStr)){
            this.code = 500;
            this.codeStr = BaseResponseCodeEnum.SYS_ERR.name();
        }
        this.message = message;
        this.data = data;
        this.errorType = errorType;
        this.errorZh = errorZh;
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>();
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data);
    }

    public static <T> BaseResponse<T> success(BaseResponseCodeEnum responseCodeEnum, T data) {
        return getInstance(responseCodeEnum.name(), responseCodeEnum.getMessage(),data);
    }

    public static <T> BaseResponse<T> error(BaseResponseCodeEnum responseCodeEnum) {
        return getInstance(responseCodeEnum.name(), responseCodeEnum.getMessage());
    }

    public static <T> BaseResponse<T> error(BaseResponseCodeEnum responseCodeEnum, T data) {
        return getInstance(responseCodeEnum.name(), responseCodeEnum.getMessage(), data);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return getInstance(code, message);
    }
    public static <T> BaseResponse<T> error(String code, String message,T data) {
        return getInstance(code, message,data);
    }
    public static <T> BaseResponse<T> error(String code, String message,T data,
            String errorType, String errorZh) {
        return getInstance(code, message, data, errorType, errorZh);
    }

    public static <T> BaseResponse<T> getInstance(String code, String message) {
        return getInstance(code, message, null);
    }

    public static <T> BaseResponse<T> getInstance(String code, String message, T data) {
        return new BaseResponse<>(code, message, data);
    }

    public static <T> BaseResponse<T> getInstance(String code, String message, T data,
            String errorType, String errorZh) {
        return new BaseResponse<>(code, message, data, errorType, errorZh);
    }


    /**
     * 解决cannot evaluate BaseResponse.toString()的exception
     * @return
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);

    }

    public JSONObject get(){
        return JSON.parseObject(data.toString());
    }
}
