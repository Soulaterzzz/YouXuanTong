package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.AuthTokenService;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.dto.LoginRequest;
import com.zs.ytbx.dto.RegisterRequest;
import com.zs.ytbx.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokenService;
    private final AuthContext authContext;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String userType = authService.login(request);
        Long userId = authService.getCurrentUserId();
        String username = authService.getCurrentUsername();
        String token = authTokenService.issueToken(userId, username, userType);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userType", userType);
        result.put("username", username);
        result.put("token", token);

        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authTokenService.revoke(authContext.getCurrentToken());
        return ApiResponse.success(null);
    }

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        SessionUser user = authContext.requireCurrentUser();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("userType", user.getUserType());
        result.put("username", user.getUsername());

        return ApiResponse.success(result);
    }
}
