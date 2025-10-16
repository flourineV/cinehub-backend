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
                                .route("auth-service", r -> r.path("/api/auth/**")
                                                .uri("http://auth-service:8081"))
                                .route("user-profile-service", r -> r.path("/api/profiles/**")
                                                .uri("http://user-profile-service:8082"))
                                .route("movie-service", r -> r.path("/api/movies/**")
                                                .uri("http://movie-service:8083"))
                                .route("showtime-service", r -> r.path("/api/showtimes/**")
                                                .uri("http://showtime-service:8084"))
                                .route("booking-service", r -> r.path("/api/bookings/**")
                                                .uri("http://booking-service:8085"))
                                .route("payment-service", r -> r.path("/api/payments/**")
                                                .uri("http://payment-service:8086"))
                                .route("pricing-service", r -> r.path("/api/pricing/**")
                                                .uri("http://pricing-service:8087"))
                                .route("fnb-service", r -> r.path("/api/fnb/**")
                                                .uri("http://fnb-service:8088"))
                                .route("promotion-service", r -> r.path("/api/promotions/**")
                                                .uri("http://promotion-service:8089"))
                                .build();
        }
}
