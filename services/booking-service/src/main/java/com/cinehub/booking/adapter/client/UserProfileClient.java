package com.cinehub.booking.adapter.client;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.math.BigDecimal;
import com.cinehub.booking.dto.external.RankAndDiscountResponse;
import com.cinehub.booking.dto.external.UserProfileResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileClient {

    @Qualifier("userProfileWebClient")
    private final WebClient userProfileWebClient;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

    @CircuitBreaker(name = "userProfileService", fallbackMethod = "fallbackRank")
    public RankAndDiscountResponse getUserRankAndDiscount(UUID userId) {
        return userProfileWebClient.get()
                .uri("/api/profiles/profiles/{userId}/rank", userId)
                .retrieve()
                .bodyToMono(RankAndDiscountResponse.class)
                .block();
    }

    public RankAndDiscountResponse fallbackRank(UUID userId, Throwable t) {
        System.err.println("Circuit Breaker activated for userProfileService. Lỗi: " + t.getMessage());
        return new RankAndDiscountResponse(userId, "BRONZE", BigDecimal.ZERO);
    }

    @CircuitBreaker(name = "userProfileService", fallbackMethod = "fallbackUpdateLoyalty")
    public void updateLoyaltyPoints(UUID userId, Integer points) {
        try {
            userProfileWebClient.patch()
                    .uri("/api/profiles/profiles/{userId}/loyalty", userId)
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(points)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Sent update loyalty points request for user {}: +{} points", userId, points);
        } catch (Exception e) {
            log.error("Failed to update loyalty points for user {}: {}", userId, e.getMessage());

        }
    }

    public void fallbackUpdateLoyalty(UUID userId, Integer points, Throwable t) {
        log.error("Circuit Breaker: Failed to update loyalty for user {}. Service might be down. Error: {}", userId,
                t.getMessage());
    }
}