package com.cinehub.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays; // Dùng Arrays cho tường minh

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 1. Cho phép Frontend (Nên thêm cả localhost:3000 nếu bạn test NextJS/React
        // khác)
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));

        // 2. Quan trọng: Max age để browser nhớ cấu hình, đỡ hỏi lại nhiều
        corsConfig.setMaxAge(3600L);

        // 3. Method và Header
        corsConfig.addAllowedMethod("*"); // Chấp nhận tất cả method (GET, POST, PUT, OPTIONS...)
        corsConfig.addAllowedHeader("*"); // Chấp nhận tất cả header

        // 4. Credentials
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}