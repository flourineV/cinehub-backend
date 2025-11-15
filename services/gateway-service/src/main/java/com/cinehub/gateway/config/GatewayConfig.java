package com.cinehub.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

        @Bean
        public ObjectMapper objectMapper() {
                return new ObjectMapper();
        }

        @Bean(name = "gatewayExecutor")
        public Executor gatewayExecutor() {
                return Executors.newFixedThreadPool(8);
        }

        // Routes are now configured in application.yml
        // Removed duplicate RouteLocator bean to avoid conflicts
}
