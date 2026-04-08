package com.zs.ytbx.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthTokenService {

    public static final Duration TOKEN_TTL = Duration.ofHours(12);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public AuthTokenService(ObjectMapper objectMapper,
                            @Value("${auth.token.secret:dev-only-change-me}") String tokenSecret) {
        if (!StringUtils.hasText(tokenSecret)) {
            throw new IllegalStateException("auth.token.secret 不能为空");
        }
        this.objectMapper = objectMapper;
        this.signingKey = tokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String issueToken(Long userId, String username, String userType) {
        Instant now = Instant.now();
        TokenPayload payload = new TokenPayload(
                UUID.randomUUID().toString().replace("-", ""),
                userId,
                userId,
                username,
                userType,
                now.toEpochMilli(),
                now.plus(TOKEN_TTL).toEpochMilli()
        );
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadPart = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signaturePart = sign(payloadPart);
            return payloadPart + "." + signaturePart;
        } catch (Exception exception) {
            throw new RuntimeException("Token生成失败", exception);
        }
    }

    public SessionUser getUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        cleanupRevokedTokens();
        if (isRevoked(token)) {
            return null;
        }

        TokenPayload payload = decodePayload(token);
        if (payload == null) {
            return null;
        }

        if (payload.getExpiresAt() <= Instant.now().toEpochMilli()) {
            return null;
        }

        return SessionUser.builder()
                .userId(payload.getUserId())
                .customerId(payload.getCustomerId())
                .username(payload.getUsername())
                .userType(payload.getUserType())
                .build();
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        TokenPayload payload = decodePayload(token);
        if (payload == null) {
            return;
        }

        revokedTokens.put(token, Instant.ofEpochMilli(payload.getExpiresAt()));
    }

    private TokenPayload decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }

        if (!verifySignature(parts[0], parts[1])) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
            return objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), TokenPayload.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean verifySignature(String payloadPart, String signaturePart) {
        try {
            byte[] expected = hmac(payloadPart);
            byte[] actual = Base64.getUrlDecoder().decode(signaturePart);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String sign(String payloadPart) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payloadPart));
    }

    private byte[] hmac(String payloadPart) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(payloadPart.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Token签名失败", exception);
        }
    }

    private boolean isRevoked(String token) {
        Instant expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(Instant.now())) {
            return true;
        }
        revokedTokens.remove(token);
        return false;
    }

    private void cleanupRevokedTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TokenPayload {
        private String tokenId;
        private Long userId;
        private Long customerId;
        private String username;
        private String userType;
        private long issuedAt;
        private long expiresAt;
    }
}
