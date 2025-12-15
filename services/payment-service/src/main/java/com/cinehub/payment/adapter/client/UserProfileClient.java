package com.cinehub.payment.adapter.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileClient {

    @Qualifier("userProfileWebClient")
    private final WebClient userProfileWebClient;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

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

            log.info("✅ Loyalty points updated for userId: {} | +{} points", userId, points);
        } catch (Exception e) {
            log.error("❌ Failed to update loyalty points for userId: {} | Error: {}", userId, e.getMessage());
            throw e; // Let circuit breaker handle retry/fallback
        }
    }

    public void fallbackUpdateLoyalty(UUID userId, Integer points, Throwable t) {
        log.warn("⚠️ Circuit Breaker activated for updateLoyaltyPoints. UserId: {} | Points: {} | Error: {}",
                userId, points, t.getMessage());
    }
}
