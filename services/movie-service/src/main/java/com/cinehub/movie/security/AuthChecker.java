package com.cinehub.movie.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AuthChecker {

    public static void requireAdmin() {
        var ctx = UserContext.get();
        if (ctx == null || !"ADMIN".equalsIgnoreCase(ctx.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    public static void requireManagerOrAdmin() {
        var ctx = UserContext.get();
        if (ctx == null ||
                (!"ADMIN".equalsIgnoreCase(ctx.getRole()) && !"MANAGER".equalsIgnoreCase(ctx.getRole()))) {
            System.out.printf("[AuthChecker] Access denied - ctx=%s, role=%s%n", 
                ctx != null ? "exists" : "null", 
                ctx != null ? ctx.getRole() : "null");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager or Admin access required");
        }
        System.out.printf("[AuthChecker] Access granted - role=%s%n", ctx.getRole());
    }

    public static void requireAuthenticated() {
        var ctx = UserContext.get();
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }

    public static String getUserIdOrThrow() {
        var ctx = UserContext.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return ctx.getUserId().toString();
    }
}
