package com.hermes.jobs.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    private static final String SUNSET_DATE = "Wed, 31 Dec 2026 23:59:59 GMT";
    private static final String DEPRECATION_DOC = "https://example.com/docs/api/versioning";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DeprecationHeaderInterceptor())
                .addPathPatterns("/api/search/**", "/api/search");
    }

    static class DeprecationHeaderInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", SUNSET_DATE);
            response.setHeader("Link", "<" + DEPRECATION_DOC + ">; rel=\"deprecation\"");
            return true;
        }
    }
}
