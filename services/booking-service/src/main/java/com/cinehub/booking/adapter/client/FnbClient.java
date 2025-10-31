package com.cinehub.booking.adapter.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cinehub.booking.dto.external.FnbItemResponse;
import com.cinehub.booking.dto.external.FnbCalculationResponse;
import com.cinehub.booking.dto.external.FnbCalculationRequest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FnbClient {

    @Qualifier("fnbWebClient")
    private final WebClient fnbWebClient;

    @CircuitBreaker(name = "fnbService", fallbackMethod = "fallbackGetFnbItemById")
    public FnbItemResponse getFnbUItemById (UUID showtimeId){
        return fnbWebClient.get()
                .uri("/api/fnb/{id}", showtimeId)
                .retrieve()
                .bodyToMono(FnbItemResponse.class)
                .block();
    }

    @CircuitBreaker(name = "fnbService", fallbackMethod = "fallbackCalculatePrice")
    public FnbCalculationResponse calculatePrice (FnbCalculationRequest request){
        return fnbWebClient.post()
                .uri("/api/fnb/calculate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FnbCalculationResponse.class)
                .block();
    }

    public FnbItemResponse fallbackGetFnbItemById(UUID showtimeId, Throwable t) {
        System.err.println("Circuit Breaker: getFnbUItemById failed. Lỗi: " + t.getMessage());
        return FnbItemResponse.builder()
                .id(showtimeId)
                .name("Unknown Item")
                .unitPrice(BigDecimal.ZERO)
                .build();
    }

    public FnbCalculationResponse fallbackCalculatePrice(FnbCalculationRequest request, Throwable t) {
        System.err.println("Circuit Breaker: calculatePrice failed. Lỗi: " + t.getMessage());
        return FnbCalculationResponse.builder()
                .totalFnbPrice(BigDecimal.ZERO)
                .calculatedFnbItems(null)
                .build();
    }

}
