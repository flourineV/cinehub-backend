package com.cinehub.gateway.config;

import com.cinehub.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

        private final JwtAuthenticationFilter jwtFilter;

        public GatewayConfig(JwtAuthenticationFilter jwtFilter) {
                this.jwtFilter = jwtFilter;
        }

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("auth-service", r -> r.path("/api/auth/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(
                                                                "/api/auth/signin,/api/auth/signup,/api/auth/refreshtoken,/api/auth/send-otp,/api/auth/reset-password"))))
                                                .uri("http://auth-service:8081"))
                                .route("user-profile-service", r -> r.path("/api/profiles/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://user-profile-service:8082"))
                                .route("movie-service", r -> r.path("/api/movies/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://movie-service:8083"))
                                .route("showtime-service", r -> r.path("/api/showtimes/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://showtime-service:8084"))
                                .route("booking-service", r -> r.path("/api/bookings/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://booking-service:8085"))
                                .route("payment-service", r -> r.path("/api/payments/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://payment-service:8086"))
                                .route("pricing-service", r -> r.path("/api/pricing/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://pricing-service:8087"))
                                .route("fnb-service", r -> r.path("/api/fnb/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://fnb-service:8088"))
                                .route("promotion-service", r -> r.path("/api/promotions/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://promotion-service:8089"))
                                .route("notification-service", r -> r.path("/api/notifications/**")
                                                .filters(f -> f.filter(jwtFilter.apply(c -> c.setExcludePaths(""))))
                                                .uri("http://notification-service:8090"))
                                .build();
        }
}
