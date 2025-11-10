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

            if (config.excludePaths != null) {
                for (String exclude : config.excludePaths) {
                    if (path.startsWith(exclude)) {
                        return chain.filter(exchange);
                    }
                }
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
            }
        }
    }
}