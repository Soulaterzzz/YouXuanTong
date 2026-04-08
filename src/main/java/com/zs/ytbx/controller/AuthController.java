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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest servletRequest,
                                                  HttpServletResponse servletResponse) {
        SessionUser authenticatedUser = authService.login(request);
        String token = authTokenService.issueToken(
                authenticatedUser.getUserId(),
                authenticatedUser.getUsername(),
                authenticatedUser.getUserType());
        addAuthCookie(servletResponse, token, servletRequest.isSecure());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", authenticatedUser.getUserId());
        result.put("userType", authenticatedUser.getUserType());
        result.put("username", authenticatedUser.getUsername());
        result.put("token", token);

        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        authTokenService.revoke(authContext.getCurrentToken());
        clearAuthCookie(servletResponse, servletRequest.isSecure());
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

    private void addAuthCookie(HttpServletResponse response, String token, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(com.zs.ytbx.common.auth.SessionConstants.AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(AuthTokenService.TOKEN_TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(com.zs.ytbx.common.auth.SessionConstants.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
