package com.cinehub.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${service.pricing.url:http://localhost:8087}")
    private String pricingServiceUrl;

    @Value("${service.fnb.url:http://localhost:8088}")
    private String fnbServiceUrl;

    @Value("${service.promotion.url:http://localhost:8089}")
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