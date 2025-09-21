package com.cinehub.gateway.filter;

import com.cinehub.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            // Skip authentication for excluded paths
            if (config.getExcludePaths() != null && 
                config.getExcludePaths().stream().anyMatch(path::contains)) {
                logger.debug("Skipping authentication for path: {}", path);
                return chain.filter(exchange);
            }
            
            // Get Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            try {
                // Extract token
                String token = authHeader.substring(7);
                
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    logger.warn("Invalid JWT token for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                
                // Extract user info from token
                UUID userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                logger.debug("Authenticated user: {} with role: {} for path: {}", userId, role, path);
                
                // Add user info to headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", role)
                        .header("X-Authenticated", "true")
                        .build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                
            } catch (Exception e) {
                logger.error("Error processing JWT token for path: {}, error: {}", path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }
    
    public static class Config {
        private List<String> excludePaths;
        
        public List<String> getExcludePaths() {
            return excludePaths;
        }
        
        public void setExcludePaths(String excludePaths) {
            this.excludePaths = Arrays.asList(excludePaths.split(","));
        }
    }
}