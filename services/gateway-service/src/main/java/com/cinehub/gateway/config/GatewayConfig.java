package com.cinehub.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .uri("http://auth-service:8081"))
                .route("movie-service", r -> r.path("/movies/**")
                        .uri("http://movie-service:8083"))
                .route("booking-service", r -> r.path("/booking/**")
                        .uri("http://booking-service:8084"))
                .build();
    }
}