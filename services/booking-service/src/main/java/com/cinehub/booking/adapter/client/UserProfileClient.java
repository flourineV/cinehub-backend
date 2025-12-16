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
import com.cinehub.booking.dto.request.UpdateLoyaltyRequest;

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
    public void updateLoyaltyPoints(UUID userId, UUID bookingId, String bookingCode, Integer points, BigDecimal amountSpent) {
        try {
            UpdateLoyaltyRequest request = new UpdateLoyaltyRequest(
                    points, 
                    bookingId, 
                    bookingCode,
                    amountSpent, 
                    "Earned points from booking"
            );
            
            userProfileWebClient.patch()
                    .uri("/api/profiles/profiles/{userId}/loyalty", userId)
                    .header("X-Internal-Secret", internalSecret)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Sent update loyalty points request for user {}: +{} points, bookingCode: {}, amount: {}", 
                    userId, points, bookingCode, amountSpent);
        } catch (Exception e) {
            log.error("Failed to update loyalty points for user {}: {}", userId, e.getMessage());

        }
    }

    public void fallbackUpdateLoyalty(UUID userId, UUID bookingId, String bookingCode, Integer points, BigDecimal amountSpent, Throwable t) {
        log.error("Circuit Breaker: Failed to update loyalty for user {}. Service might be down. Error: {}", userId,
                t.getMessage());
    }

    @CircuitBreaker(name = "userProfileService", fallbackMethod = "fallbackBatchUserNames")
    public java.util.Map<UUID, String> getBatchUserNames(java.util.List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        return userProfileWebClient.post()
                .uri("/api/profiles/profiles/batch/names")
                .header("X-Internal-Secret", internalSecret)
                .bodyValue(userIds)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<UUID, String>>() {
                })
                .block();
    }

    public java.util.Map<UUID, String> fallbackBatchUserNames(java.util.List<UUID> userIds, Throwable t) {
        log.error("Circuit Breaker: Failed to get batch user names. Error: {}", t.getMessage());
        return userIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                        userId -> userId,
                        userId -> "Unknown"));
    }

    @CircuitBreaker(name = "userProfileService", fallbackMethod = "fallbackSearchUserIds")
    public java.util.List<UUID> searchUserIdsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return userProfileWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/profiles/profiles/batch/search-userids")
                        .queryParam("username", username)
                        .build())
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.List<UUID>>() {
                })
                .block();
    }

    public java.util.List<UUID> fallbackSearchUserIds(String username, Throwable t) {
        log.error("Circuit Breaker: Failed to search user IDs by username. Error: {}", t.getMessage());
        return java.util.Collections.emptyList();
    }
}