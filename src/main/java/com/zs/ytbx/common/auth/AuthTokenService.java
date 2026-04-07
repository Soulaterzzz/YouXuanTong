package com.zs.ytbx.common.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final Map<String, TokenSession> tokenStore = new ConcurrentHashMap<>();

    public String issueToken(Long userId, String username, String userType) {
        String token = UUID.randomUUID().toString().replace("-", "");
        SessionUser user = SessionUser.builder()
                .userId(userId)
                .customerId(userId)
                .username(username)
                .userType(userType)
                .build();
        tokenStore.put(token, new TokenSession(user, LocalDateTime.now().plus(TOKEN_TTL)));
        return token;
    }

    public SessionUser getUser(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        TokenSession tokenSession = tokenStore.get(token);
        if (tokenSession == null) {
            return null;
        }

        if (tokenSession.expiredAt().isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return null;
        }

        return tokenSession.user();
    }

    public void revoke(String token) {
        if (StringUtils.hasText(token)) {
            tokenStore.remove(token);
        }
    }

    private record TokenSession(SessionUser user, LocalDateTime expiredAt) {
    }
}
