package com.zs.ytbx.config;

import com.zs.ytbx.common.auth.SessionConstants;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getSession(false) == null || request.getSession(false).getAttribute(SessionConstants.SESSION_USER) == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return true;
    }
}
