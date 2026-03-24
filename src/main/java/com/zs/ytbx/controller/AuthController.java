package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.auth.SessionConstants;
import com.zs.ytbx.dto.LoginRequest;
import com.zs.ytbx.dto.RegisterRequest;
import com.zs.ytbx.service.AuthService;
import jakarta.servlet.http.HttpSession;
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

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        String userType = authService.login(request);
        Long userId = authService.getCurrentUserId();
        String username = authService.getCurrentUsername();
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userType", userType);
        result.put("username", username);
        
        session.setAttribute(SessionConstants.SESSION_USER_ID, userId);
        session.setAttribute(SessionConstants.SESSION_USER_TYPE, userType);
        session.setAttribute(SessionConstants.SESSION_USERNAME, username);
        
        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success(null);
    }

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionConstants.SESSION_USER_ID);
        String userType = (String) session.getAttribute(SessionConstants.SESSION_USER_TYPE);
        String username = (String) session.getAttribute(SessionConstants.SESSION_USERNAME);
        
        if (userId == null || username == null) {
            return ApiResponse.fail("未登录");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userType", userType);
        result.put("username", username);
        
        return ApiResponse.success(result);
    }
}
