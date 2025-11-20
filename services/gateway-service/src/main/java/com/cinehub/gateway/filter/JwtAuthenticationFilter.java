package com.cinehub.gateway.filter;

import com.cinehub.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j

public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.info("JwtFilter checking path: {}", path);

            if (config.excludePaths != null) {
                log.info("Exclude paths configured: {}", config.excludePaths);
                for (String exclude : config.excludePaths) {
                    if (matchesPath(path, exclude)) {
                        log.info("Path {} matched exclude pattern: {}, skipping auth", path, exclude);
                        return chain.filter(exchange);
                    }
                }
                log.info("Path {} did not match any exclude patterns", path);
            } else {
                log.warn("No exclude paths configured for this route");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path {}", path);
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            try {
                // Extract token
                String token = authHeader.substring(7);

                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid JWT token at path {}", path);
                    return unauthorized(exchange, "Invalid JWT token");
                }

                // Extract user info from token
                UUID userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                // Gắn info user vào header
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Authenticated", "true")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("Error processing JWT for path {} -> {}", path, e.getMessage());
                return unauthorized(exchange, "Error verifying JWT");
            }
        };
    }

    private boolean matchesPath(String path, String pattern) {
        // Exact match
        if (path.equals(pattern)) {
            log.debug("Exact match: {} == {}", path, pattern);
            return true;
        }

        // Wildcard pattern matching
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            boolean matches = path.startsWith(prefix);
            log.debug("Pattern /** - prefix: '{}', path: '{}', matches: {}", prefix, path, matches);
            return matches;
        }

        // Pattern with */something - matches UUID/something
        if (pattern.contains("/*/")) {
            String[] patternParts = pattern.split("/\\*/");
            if (patternParts.length == 2) {
                String prefix = patternParts[0];
                String suffix = patternParts[1];
                if (path.startsWith(prefix + "/") && path.endsWith("/" + suffix)) {
                    String middle = path.substring(prefix.length() + 1, path.length() - suffix.length() - 1);
                    // Check if middle part is a single segment (no slashes) and looks like UUID
                    boolean matches = !middle.contains("/") && middle.matches("[0-9a-f-]+");
                    log.debug("Pattern /*/ - prefix: '{}', suffix: '{}', middle: '{}', matches: {}", prefix, suffix, middle, matches);
                    return matches;
                }
            }
            return false;
        }

        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1); // Keep the trailing /
            if (path.startsWith(prefix) && path.length() > prefix.length()) {
                String remaining = path.substring(prefix.length());
                // No additional slashes after the single-level match
                boolean matches = !remaining.contains("/");
                log.debug("Pattern /* - prefix: '{}', remaining: '{}', matches: {}", prefix, remaining, matches);
                return matches;
            }
            return false;
        }

        // Special pattern: /{uuid} - matches UUID paths only
        if (pattern.endsWith("/{uuid}")) {
            String prefix = pattern.substring(0, pattern.length() - 7); // Remove /{uuid}
            if (path.startsWith(prefix + "/") && path.length() > prefix.length() + 1) {
                String remaining = path.substring(prefix.length() + 1);
                // Check if remaining part is a UUID (8-4-4-4-12 format with hyphens)
                boolean matches = remaining.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
                log.debug("Pattern /{uuid} - prefix: '{}', remaining: '{}', matches: {}", prefix, remaining, matches);
                return matches;
            }
            return false;
        }

        // No match
        log.debug("No match - pattern: '{}', path: '{}'", pattern, path);
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        log.debug("Unauthorized access: {}", message);
        return exchange.getResponse().setComplete();
    }

    public static class Config {

        private List<String> excludePaths;

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(String excludePaths) {
            if (excludePaths != null && !excludePaths.isEmpty()) {
                this.excludePaths = Arrays.asList(excludePaths.split(","));
                log.info("Parsed exclude paths from String: {}", this.excludePaths);
            }
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
            log.info("Set exclude paths from List: {}", this.excludePaths);
        }
    }
}