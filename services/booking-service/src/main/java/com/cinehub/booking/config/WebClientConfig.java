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
}