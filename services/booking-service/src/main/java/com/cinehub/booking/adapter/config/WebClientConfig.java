package com.cinehub.booking.adapter.config;

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

    @Value("${pricing.service.url}")
    private String pricingServiceUrl;

    @Value("${fnb.service.url}")
    private String fnbServiceUrl;

    @Value("${promotion.service.url}")
    private String promotionServiceUrl;

    @Value("${movie.service.url}")
    private String movieServiceUrl;

    @Value("${showtime.service.url}")
    private String showtimeServiceUrl;

    @Value("${user-profile.service.url}")
    private String userProfileServiceUrl;

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
    public WebClient movieWebClient(WebClient.Builder builder) {
        return builder.baseUrl(movieServiceUrl).build();
    }

    @Bean
    public WebClient userProfileWebClient(WebClient.Builder builder) {
        return builder.baseUrl(userProfileServiceUrl).build();
    }

    @Bean
    public WebClient showtimeWebClient(WebClient.Builder builder) {
        return builder.baseUrl(showtimeServiceUrl).build();
    }

    @Bean
    public WebClient pricingWebClient(WebClient.Builder builder) {
        return builder.baseUrl(pricingServiceUrl).build();
    }

    @Bean
    public WebClient fnbWebClient(WebClient.Builder builder) {
        return builder.baseUrl(fnbServiceUrl).build();
    }

    @Bean
    public WebClient promotionWebClient(WebClient.Builder builder) {
        return builder.baseUrl(promotionServiceUrl).build();
    }
}