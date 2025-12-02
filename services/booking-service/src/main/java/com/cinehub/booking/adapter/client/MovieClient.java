package com.cinehub.booking.adapter.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cinehub.booking.dto.external.MovieTitleResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieClient {

    @Qualifier("movieWebClient")
    private final WebClient movieWebClient;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

    @CircuitBreaker(name = "movieService", fallbackMethod = "fallbackMovie")
    public MovieTitleResponse getMovieTitleById(UUID movieId) {
        return movieWebClient.get()
                .uri("/api/movies/{id}", movieId)
                .retrieve()
                .bodyToMono(MovieTitleResponse.class)
                .block();
    }

    public MovieTitleResponse fallbackMovie(UUID movieId, Throwable t) {
        System.err.println("Circuit Breaker activated for movieService. Lỗi: " + t.getMessage());
        return MovieTitleResponse.builder()
                .id(movieId)
                .title(null)
                .build();
    }

    @CircuitBreaker(name = "movieService", fallbackMethod = "fallbackBatchMovieTitles")
    public java.util.Map<UUID, String> getBatchMovieTitles(java.util.List<UUID> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        return movieWebClient.post()
                .uri("/api/movies/batch/titles")
                .header("X-Internal-Secret", internalSecret)
                .bodyValue(movieIds)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.Map<UUID, String>>() {
                })
                .block();
    }

    public java.util.Map<UUID, String> fallbackBatchMovieTitles(java.util.List<UUID> movieIds, Throwable t) {
        log.error("Circuit Breaker: Failed to get batch movie titles. Error: {}", t.getMessage());
        return movieIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                        movieId -> movieId,
                        movieId -> "Unknown Movie"));
    }
}
