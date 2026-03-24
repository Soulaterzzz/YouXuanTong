package com.zs.ytbx.common.auth;

import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthContext {

    public SessionUser getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request.getSession(false) == null) {
            return null;
        }
        return (SessionUser) request.getSession(false).getAttribute(SessionConstants.SESSION_USER);
    }

    public SessionUser requireCurrentUser() {
        SessionUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
