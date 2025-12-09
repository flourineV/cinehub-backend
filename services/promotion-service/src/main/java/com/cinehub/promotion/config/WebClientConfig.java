package com.cinehub.promotion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunctions.statusError(
                        HttpStatusCode::is4xxClientError,
                        resp -> new RuntimeException("Client error: " + resp.statusCode())))
                .filter(ExchangeFilterFunctions.statusError(
                        HttpStatusCode::is5xxServerError,
                        resp -> new RuntimeException("Server error: " + resp.statusCode())));
    }

    @Bean
    public WebClient notificationWebClient(WebClient.Builder builder) {
        return builder.baseUrl(notificationServiceUrl).build();
    }
}
