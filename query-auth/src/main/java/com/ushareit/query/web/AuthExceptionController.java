package com.ushareit.query.web;

import javax.servlet.http.HttpServletRequest;

import com.ushareit.query.constant.AuthResponseCodeEnum;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.web.vo.BaseResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wuyan
 * @date 2019/8/8
 **/
@Slf4j
@RestController
@RequestMapping("authFilterExceptionController")
public class AuthExceptionController {

    @RequestMapping
    public BaseResponse filterExceptionHandler(HttpServletRequest request) throws Throwable {
        Object attribute = request.getAttribute(CommonConstant.AUTH_EXCEPTION);

        if (attribute instanceof BaseResponseCodeEnum) {
            BaseResponseCodeEnum baseCodeEnum = (BaseResponseCodeEnum) attribute;

            if (baseCodeEnum == null) {
                baseCodeEnum = BaseResponseCodeEnum.NO_RIGHT;
            }
            log.error("auth error: code {}, message {}", baseCodeEnum.name(), baseCodeEnum.getMessage());
            return BaseResponse.error(baseCodeEnum.name(),baseCodeEnum.getMessage());
        }


        AuthResponseCodeEnum authCodeEnum = (AuthResponseCodeEnum) attribute;
        if (authCodeEnum == null) {
            authCodeEnum = AuthResponseCodeEnum.AUTH_ERROR;
        }
        log.error("auth error: code {}, message {}", authCodeEnum.name(), authCodeEnum.getMessage());
        return BaseResponse.error(authCodeEnum.name(),authCodeEnum.getMessage());

    }
}
