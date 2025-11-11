package com.cinehub.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi movieApi() {
        return GroupedOpenApi.builder()
                .group("movie-service")
                .pathsToMatch("/api/movie/**")
                .build();
    }

    // ... thêm service khác
}
