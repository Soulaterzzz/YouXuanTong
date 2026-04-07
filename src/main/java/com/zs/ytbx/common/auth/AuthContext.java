package com.zs.ytbx.common.auth;

import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuthContext {

    private final AuthTokenService authTokenService;

    public String getCurrentToken() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String authorization = request.getHeader(SessionConstants.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(SessionConstants.TOKEN_PREFIX)) {
            return null;
        }

        return authorization.substring(SessionConstants.TOKEN_PREFIX.length()).trim();
    }

    public SessionUser getCurrentUser() {
        String token = getCurrentToken();
        SessionUser tokenUser = authTokenService.getUser(token);
        if (tokenUser != null) {
            return tokenUser;
        }

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute(SessionConstants.SESSION_USER);
        if (sessionUser instanceof SessionUser user) {
            return user;
        }

        Long userId = (Long) session.getAttribute(SessionConstants.SESSION_USER_ID);
        String username = (String) session.getAttribute(SessionConstants.SESSION_USERNAME);
        String userType = (String) session.getAttribute(SessionConstants.SESSION_USER_TYPE);
        if (userId == null || !StringUtils.hasText(username)) {
            return null;
        }

        return SessionUser.builder()
                .userId(userId)
                .customerId(userId)
                .username(username)
                .userType(userType)
                .build();
    }

    public SessionUser requireCurrentUser() {
        SessionUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }
}
