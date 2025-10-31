package com.cinehub.booking.adapter.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;
import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.exception.BookingException; 
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono; 

@Service
@RequiredArgsConstructor
public class PricingClient {

    @Qualifier("pricingWebClient")
    private final WebClient pricingWebClient;

    @CircuitBreaker(name = "pricingService", fallbackMethod = "fallbackSeatPrice")
    public SeatPriceResponse getSeatPrice(String seatType, String ticketType) {
        
        return pricingWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/pricing/seat-price")
                        .queryParam("seatType", seatType)
                        .queryParam("ticketType", ticketType)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    return Mono.error(new BookingException(
                            "Không tìm thấy mức giá cho loại ghế/vé này."));
                })
                .bodyToMono(SeatPriceResponse.class)
                .block(); 
    }

    public SeatPriceResponse fallbackSeatPrice(String seatType, String ticketType, Throwable t) {
        
        System.err.println("Circuit Breaker activated for pricingService. Lỗi: " + t.getMessage());
  
        return SeatPriceResponse.builder()
                .seatType(seatType)
                .ticketType(ticketType)
                .basePrice(BigDecimal.ZERO) 
                .build();
    }
}