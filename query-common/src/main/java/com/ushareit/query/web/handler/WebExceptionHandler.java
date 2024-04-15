package com.ushareit.query.web.handler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;

import com.ushareit.query.web.vo.BaseResponse;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alibaba.fastjson.JSONException;

import lombok.extern.slf4j.Slf4j;

/**
 * 用于处理通用异常
 *
 * @author swq
 * @date 2018/10/16
 */
@Slf4j
@AutoConfigureAfter
@RestControllerAdvice
public class WebExceptionHandler {

    /**
     * 忽略无法处理的异常
     * HttpMessageNotReadableException:包括 EOFException SocketTimeoutException
     * HttpMediaTypeNotSupportedException:Content_type not supported
     * HttpRequestMethodNotSupportedException: Request method ‘HEAD/GET...’ not supported
     * ClientAbortException: Broke pipe
     * JSONException: 参数JSON转换异常
     * IllegalArgumentException:Base64 decode 异常
     *
     * @param request   请求对象
     * @param throwable 异常对象
     * @return
     */
    @ExceptionHandler(value = {HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class,
            HttpRequestMethodNotSupportedException.class, ClientAbortException.class, JSONException.class,
            IllegalArgumentException.class})
    public BaseResponse ignoreException(HttpServletRequest request, Throwable throwable) {
        log.error("ignore error:", throwable);
        return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR);
    }

    /**
     * bean validate exception
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse beanValidate(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder errorMessage = new StringBuilder("参数校验失败：");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMessage.append(fieldError.getDefaultMessage());
        }
        return BaseResponse.error(BaseResponseCodeEnum.CLI_PARAM_ILLEGAL, errorMessage.toString());
    }

    /**
     * request binding , validate bean
     */
    @ExceptionHandler({
            ServletRequestBindingException.class,
            ValidationException.class
    })
    public BaseResponse beanValidate(Exception e) {
        log.error("bean validate  error:", e);
        return BaseResponse.error(BaseResponseCodeEnum.CLI_PARAM_ILLEGAL,e.getMessage());
    }

    /**
     * 通用Exception异常处理
     *
     * @param e Exception
     * @return BaseResponse
     */
    @ExceptionHandler(Throwable.class)
    public BaseResponse throwable(Throwable e) {
        log.error(BaseResponseCodeEnum.SYS_ERR.toString(), e);
        return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR);
    }

    /**
     * ServiceException异常处理
     *
     * @param e Exception
     * @return BaseResponse
     */
    @ExceptionHandler(ServiceException.class)
    public BaseResponse serviceException(Throwable e) {
        log.error("service error:", e);
        return BaseResponse.error(((ServiceException) e).getCodeStr(),((ServiceException) e).getMessage(),((ServiceException) e).getData());
    }
}
