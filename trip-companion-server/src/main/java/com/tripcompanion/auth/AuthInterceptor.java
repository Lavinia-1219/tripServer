package com.tripcompanion.auth;

import com.tripcompanion.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "authUserId";
    public static final String ATTR_TOKEN = "authToken";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("请先登录");
        }

        Long userId = authService.resolveUserId(token);
        if (userId == null) {
            throw ApiException.unauthorized("登录已过期，请重新登录");
        }

        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_TOKEN, token);
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
