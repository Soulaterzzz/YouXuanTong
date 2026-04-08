package com.zs.ytbx.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class AuthTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);
    private static final String TOKEN_PREFIX = "ytbx:token:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String issueToken(Long userId, String username, String userType) {
        String token = UUID.randomUUID().toString().replace("-", "");
        SessionUser user = SessionUser.builder()
                .userId(userId)
                .customerId(userId)
                .username(username)
                .userType(userType)
                .build();
        try {
            String userJson = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(TOKEN_PREFIX + token, userJson, TOKEN_TTL);
        } catch (Exception e) {
            throw new RuntimeException("Token存储失败", e);
        }
        return token;
    }

    public SessionUser getUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String userJson = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(userJson, SessionUser.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }
}