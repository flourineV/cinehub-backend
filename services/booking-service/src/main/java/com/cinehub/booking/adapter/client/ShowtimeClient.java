package com.cinehub.booking.adapter.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cinehub.booking.dto.external.ShowtimeDetailResponse;
import com.cinehub.booking.dto.external.ShowtimeResponse;
import com.cinehub.booking.dto.external.SeatResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowtimeClient {

    @Qualifier("showtimeWebClient")
    private final WebClient showtimeWebClient;

    @CircuitBreaker(name = "showtimeService", fallbackMethod = "fallbackGetShowtimeById")
    public ShowtimeResponse getShowtimeById(UUID showtimeId) {
        return showtimeWebClient.get()
                .uri("/api/showtimes/{id}", showtimeId)
                .retrieve()
                .bodyToMono(ShowtimeResponse.class)
                .block();
    }

    @CircuitBreaker(name = "showtimeService", fallbackMethod = "fallbackGetSeatInfoById")
    public SeatResponse getSeatInfoById(UUID seatId) {
        return showtimeWebClient.get()
                .uri("/api/showtimes/seats/{id}", seatId)
                .retrieve()
                .bodyToMono(SeatResponse.class)
                .block();
    }

    @CircuitBreaker(name = "showtimeService", fallbackMethod = "fallbackGetShowtimesByFilter")
    public List<ShowtimeDetailResponse> getShowtimesByFilter(
            UUID provinceId, UUID theaterId, LocalDate startOfDay, LocalDate endOfDay) {

        return showtimeWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/showtimes/admin/search")
                            .queryParam("page", 1)
                            .queryParam("size", 10000); // Get all

                    if (provinceId != null) {
                        builder.queryParam("provinceId", provinceId);
                    }
                    if (theaterId != null) {
                        builder.queryParam("theaterId", theaterId);
                    }
                    if (startOfDay != null) {
                        builder.queryParam("startOfDay", startOfDay);
                    }
                    if (endOfDay != null) {
                        builder.queryParam("endOfDay", endOfDay);
                    }

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PagedResponse<ShowtimeDetailResponse>>() {
                })
                .map(PagedResponse::getData)
                .block();
    }

    public ShowtimeResponse fallbackGetShowtimeById(UUID showtimeId, Throwable t) {
        System.err.println("Circuit Breaker: getShowtimeById failed → " + t.getMessage());
        return ShowtimeResponse.builder()
                .id(showtimeId)
                .movieId(null)
                .theaterName("Unknown Theater")
                .roomName("Unknown Room")
                .startTime(null)
                .endTime(null)
                .price(BigDecimal.ZERO)
                .build();
    }

    public SeatResponse fallbackGetSeatInfoById(UUID seatId, Throwable t) {
        System.err.println("Circuit Breaker: getSeatInfoById failed → " + t.getMessage());
        return SeatResponse.builder()
                .id(seatId)
                .seatNumber("N/A")
                .roomName("Unknown Room")
                .build();
    }

    public List<ShowtimeDetailResponse> fallbackGetShowtimesByFilter(
            UUID provinceId, UUID theaterId, LocalDate startOfDay, LocalDate endOfDay, Throwable t) {
        System.err.println("Circuit Breaker: getShowtimesByFilter failed → " + t.getMessage());
        return Collections.emptyList();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PagedResponse<T> {
        private List<T> data;
        private int currentPage;
        private int totalPages;
        private long totalItems;
    }
}
