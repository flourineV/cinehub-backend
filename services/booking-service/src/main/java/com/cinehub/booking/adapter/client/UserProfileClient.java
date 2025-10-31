package com.cinehub.booking.adapter.client;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Qualifier;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker; 
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.math.BigDecimal;
import com.cinehub.booking.dto.external.RankAndDiscountResponse;

@Service
@RequiredArgsConstructor
public class UserProfileClient {
    
    @Qualifier("userProfileWebClient")
    private final WebClient userProfileWebClient;

    @CircuitBreaker(name = "userProfileService", fallbackMethod = "fallbackRank")
    public RankAndDiscountResponse getUserRankAndDiscount (UUID userId){
        return userProfileWebClient.get()
                .uri("/api/profiles/profiles/{userId}/rank", userId)
                .retrieve()
                .bodyToMono(RankAndDiscountResponse.class)
                .block(); 
    }

    public RankAndDiscountResponse fallbackRank(UUID userId, Throwable t){
        System.err.println("Circuit Breaker activated for userProfileService. Lỗi: " + t.getMessage()); 
        return new RankAndDiscountResponse(userId, "BRONZE", BigDecimal.ZERO);
    }
}