package com.cinehub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

        public static void main(String[] args) {
                SpringApplication.run(GatewayApplication.class, args);
        }

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("auth-service", r -> r
                                                .path("/api/auth/**")
                                                .uri("http://localhost:8081"))
                                .route("user-profile-service", r -> r
                                                .path("/api/profiles/**")
                                                .uri("http://localhost:8082"))
                                .route("movie-service", r -> r
                                                .path("/api/movies/**")
                                                .uri("http://localhost:8083"))
                                .route("showtime-service", r -> r
                                                .path("/api/showtimes/**")
                                                .uri("http://showtime-service:8084"))
                                .route("booking-service", r -> r
                                                .path("/api/bookings/**")
                                                .uri("http://booking-service:8085"))
                                .route("payment-service", r -> r
                                                .path("/api/payments/**")
                                                .uri("http://payment-service:8086"))
                                .route("pricing-service", r -> r
                                                .path("/api/pricing/**")
                                                .uri("http://pricing-service:8087"))
                                .route("fnb-service", r -> r
                                                .path("/api/fnb/**")
                                                .uri("http://fnb-service:8088"))
                                .route("promotion-service", r -> r
                                                .path("/api/promotions/**")
                                                .uri("http://promotion-service:8089"))
                                .route("notification-service", r -> r
                                                .path("/api/notifications/**")
                                                .uri("http://notification-service:8090"))
                                .build();
        }
}