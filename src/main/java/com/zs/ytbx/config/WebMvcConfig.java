package com.zs.ytbx.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/user/**",
                        "/api/policies/**",
                        "/api/claims/**",
                        "/api/consult-orders/**",
                        "/api/advisors/**",
                        "/api/auth/me",
                        "/api/auth/logout"
                );
    }
}
