package com.zs.ytbx.controller;

import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.AuthTokenService;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.LoginRequest;
import com.zs.ytbx.service.AuthService;
import com.zs.ytbx.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest extends ControllerTestSupport {

    @Mock
    private AuthService authService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthContext authContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(new AuthController(authService, authTokenService, authContext));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin111");

        given(authService.login(any(LoginRequest.class))).willReturn(SessionUser.builder()
                .userId(1L)
                .username("admin")
                .userType("ADMIN")
                .build());
        given(authTokenService.issueToken(1L, "admin", "ADMIN")).willReturn("token-001");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.userType").value("ADMIN"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").value("token-001"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("ytbx-auth-token=token-001")));
    }

    @Test
    void shouldValidateLoginRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.data").value("用户名不能为空"));
    }

    @Test
    void shouldReturnUnauthorizedWhenCurrentUserMissing() throws Exception {
        given(authContext.requireCurrentUser()).willThrow(new BusinessException(ResultCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    void shouldReturnCurrentUserSuccessfully() throws Exception {
        given(authContext.requireCurrentUser()).willReturn(SessionUser.builder()
                .userId(7L)
                .username("user7")
                .userType("USER")
                .build());

        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(7L))
                .andExpect(jsonPath("$.data.username").value("user7"))
                .andExpect(jsonPath("$.data.userType").value("USER"));
    }

    @Test
    void shouldRevokeCurrentTokenWhenLogout() throws Exception {
        given(authContext.getCurrentToken()).willReturn("token-logout");

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("ytbx-auth-token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authTokenService).revoke("token-logout");
    }
}
