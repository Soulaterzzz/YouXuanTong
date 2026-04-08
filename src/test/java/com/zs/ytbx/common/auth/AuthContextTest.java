package com.zs.ytbx.common.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthContextTest {

    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final AuthContext authContext = new AuthContext(authTokenService);

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReadTokenFromCookieWhenAuthorizationHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(SessionConstants.AUTH_COOKIE_NAME, "cookie-token"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(authTokenService.getUser("cookie-token")).thenReturn(SessionUser.builder()
                .userId(8L)
                .customerId(8L)
                .username("user8")
                .userType("USER")
                .build());

        assertThat(authContext.getCurrentToken()).isEqualTo("cookie-token");
        assertThat(authContext.requireCurrentUser().getUsername()).isEqualTo("user8");
    }

    @Test
    void shouldPreferAuthorizationHeaderOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SessionConstants.AUTHORIZATION_HEADER, SessionConstants.TOKEN_PREFIX + "header-token");
        request.setCookies(new jakarta.servlet.http.Cookie(SessionConstants.AUTH_COOKIE_NAME, "cookie-token"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(authTokenService.getUser("header-token")).thenReturn(SessionUser.builder()
                .userId(9L)
                .customerId(9L)
                .username("user9")
                .userType("USER")
                .build());

        assertThat(authContext.getCurrentToken()).isEqualTo("header-token");
        assertThat(authContext.requireCurrentUser().getUserId()).isEqualTo(9L);
    }
}
