package com.ushareit.query.web.interceptor;

import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.constant.DsTaskConstant;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wuyan
 * @date 2018/11/14
 **/
@Slf4j
@ControllerAdvice
public class HttpResponseTraceAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public Object beforeBodyWrite(Object object,
                                  @NonNull MethodParameter methodParameter,
                                  @NonNull MediaType mediaType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> clas,
                                  @NonNull ServerHttpRequest serverHttpRequest,
                                  @NonNull ServerHttpResponse serverHttpResponse) {
        //通过 ServerHttpRequest的实现类ServletServerHttpRequest 获得HttpServletRequest
        ServletServerHttpRequest sshreq = (ServletServerHttpRequest) serverHttpRequest;
        HttpServletRequest request = sshreq.getServletRequest();
        if (request.getAttribute(CommonConstant.AUTH_EXCEPTION) == null) {
            recordRepsonseLog(request, object);
        }
        /*ServletServerHttpResponse response = (ServletServerHttpResponse) serverHttpResponse;
        response.getHeaders().add("Access-Control-Request-Private-Network", "true");
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Headers", "*");
        response.getHeaders().add("Access-Control-Allow-Methods", "*");
        response.getHeaders().add("Access-Control-Expose-Headers", "*");*/
        return object;
    }

    private void recordRepsonseLog(HttpServletRequest request, Object responseObject) {
        boolean match = WebUtils.requestAnyIsMatch(request, DsTaskConstant.IGNORE_METHOD, null, DsTaskConstant.IGNORE_INTERCEPT_PATHS);
        if (match) {
            return;
        }

        //获取请求操作类型
        String method = request.getMethod();
        BaseResponseCodeEnum responceCode = isMatchMethod(method);

        String response = responseObject.toString();
        if (response.length() > DsTaskConstant.EVENT_RESPONCE_LENGTH) {
            response = response.substring(0, DsTaskConstant.EVENT_RESPONCE_LENGTH);
        }


        InfTraceContextHolder.remove();
    }

    @Override
    public boolean supports(@NonNull MethodParameter methodParameter, @NonNull Class clas) {
        return true;
    }

    /**
     * 判断每个请求是用来做什么的，方便写入event中的event_code
     *
     * @param path
     * @return
     */
    private BaseResponseCodeEnum isMatchMethod(String path) {
        switch (path) {
            case DsTaskConstant.POST_METHOD:
                return BaseResponseCodeEnum.CREATED;
            case DsTaskConstant.DELETE_METHOD:
                return BaseResponseCodeEnum.DELETED;
            case DsTaskConstant.PUT_METHOD:
                return BaseResponseCodeEnum.UPDATED_ALL;
            case DsTaskConstant.PATCH_METHOD:
                return BaseResponseCodeEnum.UPDATED;
            case DsTaskConstant.GET_METHOD:
                return BaseResponseCodeEnum.QUERY;
            default:
                return BaseResponseCodeEnum.UNKOWN;
        }
    }
}

