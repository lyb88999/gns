package com.gns.notification.config;

import com.gns.notification.security.UserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    public WebMvcConfig(UserContextInterceptor userContextInterceptor) {
        this.userContextInterceptor = userContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/public/**")
            .excludePathPatterns("/api/v1/auth/**") // Exclude auth endpoints
            .excludePathPatterns("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**");
    }
}
