package com.cinehub.booking.adapter.client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode; // Import này cần thiết cho onStatus
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.entity.DiscountType;
import com.cinehub.booking.exception.BookingException; // Giả định
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono; // Cần thiết cho onStatus và Mono.error

@Service
@RequiredArgsConstructor
public class PromotionClient {

    @Qualifier("promotionWebClient")
    private final WebClient promotionWebClient;

    @CircuitBreaker(name = "promotionService", fallbackMethod = "fallbackPromotion") 
    public PromotionValidationResponse validatePromotionCode(String promoCode) { 
        
        return promotionWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/promotions/validate")
                        .queryParam("code", promoCode)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {

                    return Mono.error(new BookingException(
                            "Mã khuyến mãi không hợp lệ hoặc đã hết hạn."));
                })
                .bodyToMono(PromotionValidationResponse.class)
                .block(); 
    }

    public PromotionValidationResponse fallbackPromotion(String promoCode, Throwable t) { 
        
        System.err.println("Circuit Breaker activated for promotionService. Lỗi: " + t.getMessage());
        
        return PromotionValidationResponse.builder()
                .code(promoCode)
                .discountType(DiscountType.PERCENTAGE) 
                .discountValue(BigDecimal.ZERO)
                .isOneTimeUse(Boolean.FALSE)
                .build();
    }
}