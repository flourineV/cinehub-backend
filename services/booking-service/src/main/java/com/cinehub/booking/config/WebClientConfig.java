package com.cinehub.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public WebClient movieWebClient() {
        return WebClient.builder()
                .baseUrl(movieServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient showtimeWebClient() {
        return WebClient.builder()
                .baseUrl(showtimeServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient pricingWebClient() {
        return WebClient.builder()
                .baseUrl(pricingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient fnbWebClient() {
        return WebClient.builder()
                .baseUrl(fnbServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient promotionWebClient() {
        return WebClient.builder()
                .baseUrl(promotionServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient userProfileWebClient() {
        return WebClient.builder()
                .baseUrl(userProfileServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}