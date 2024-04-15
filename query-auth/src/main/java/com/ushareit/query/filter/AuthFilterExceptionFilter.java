package com.ushareit.query.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wuyan
 * @date 2019/8/7
 **/
@Slf4j
@Component
public class AuthFilterExceptionFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                 FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable e) {
            log.error("filter has error: ", e);
            request.getRequestDispatcher("/authFilterExceptionController").forward(request, response);
        }
    }
}
