package com.cinehub.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi profileApi() {
        return GroupedOpenApi.builder()
                .group("user-profile-service")
                .pathsToMatch("/api/profiles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi movieApi() {
        return GroupedOpenApi.builder()
                .group("movie-service")
                .pathsToMatch("/api/movies/**")
                .build();
    }

    @Bean
    public GroupedOpenApi showtimeApi() {
        return GroupedOpenApi.builder()
                .group("showtime-service")
                .pathsToMatch("/api/showtimes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("booking-service")
                .pathsToMatch("/api/bookings/**")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("payment-service")
                .pathsToMatch("/api/payments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi pricingApi() {
        return GroupedOpenApi.builder()
                .group("pricing-service")
                .pathsToMatch("/api/pricing/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fnbApi() {
        return GroupedOpenApi.builder()
                .group("fnb-service")
                .pathsToMatch("/api/fnb/**")
                .build();
    }

    @Bean
    public GroupedOpenApi promotionApi() {
        return GroupedOpenApi.builder()
                .group("promotion-service")
                .pathsToMatch("/api/promotions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .pathsToMatch("/api/notifications/**")
                .build();
    }
}
