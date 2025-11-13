package com.cinehub.payment.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String authenticated = request.getHeader("X-Authenticated");

        UserContext.set(new UserContext(
                userId,
                role,
                "true".equalsIgnoreCase(authenticated)));

        System.out.printf("[AuthService] Request from user=%s, role=%s, authenticated=%s%n",
                userId, role, authenticated);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        UserContext.clear();
    }
}
