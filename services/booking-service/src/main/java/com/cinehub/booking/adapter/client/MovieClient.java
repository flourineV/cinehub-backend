package com.cinehub.booking.adapter.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.cinehub.booking.dto.external.MovieTitleResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieClient {

    @Qualifier("movieWebClient")
    private final WebClient movieWebClient;

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
}
