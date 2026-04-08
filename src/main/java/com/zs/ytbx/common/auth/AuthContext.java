package com.zs.ytbx.common.auth;

import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthContext {

    private final AuthTokenService authTokenService;

    public String getCurrentToken() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        List<String> tokenCandidates = getTokenCandidates(request);
        return tokenCandidates.isEmpty() ? null : tokenCandidates.get(0);
    }

    public SessionUser getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        for (String token : getTokenCandidates(request)) {
            SessionUser tokenUser = authTokenService.getUser(token);
            if (tokenUser != null) {
                return tokenUser;
            }
        }
        return null;
    }

    public SessionUser requireCurrentUser() {
        SessionUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private List<String> getTokenCandidates(HttpServletRequest request) {
        List<String> tokens = new ArrayList<>(2);

        String authorization = request.getHeader(SessionConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authorization) && authorization.startsWith(SessionConstants.TOKEN_PREFIX)) {
            String token = authorization.substring(SessionConstants.TOKEN_PREFIX.length()).trim();
            if (StringUtils.hasText(token)) {
                tokens.add(token);
            }
        }

        String cookieToken = getCookieToken(request);
        if (StringUtils.hasText(cookieToken) && !tokens.contains(cookieToken)) {
            tokens.add(cookieToken);
        }

        return tokens;
    }

    private String getCookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (cookie != null && SessionConstants.AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }
}
