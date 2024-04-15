package com.ushareit.query.service.third;

/**
 * @author zhaopan
 * @date 2021/2/3
 */
public interface OIDCIdentityService {

    /**
     * 针对需要OIDC校验的请求，判断请求携带的token是否有效
     *
     * @param token 请求携带的token
     * @return token是否有效
     */
    boolean validate(String token);

}
