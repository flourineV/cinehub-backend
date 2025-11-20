package com.cinehub.showtime.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieServiceClient {

    private final RestTemplate restTemplate;
    private static final String MOVIE_SERVICE_URL = "http://movie-service:8083/api/movies";

    public String getMovieTitle(UUID movieId) {
        try {
            String url = MOVIE_SERVICE_URL + "/" + movieId;
            MovieResponse response = restTemplate.getForObject(url, MovieResponse.class);
            return response != null ? response.getTitle() : null;
        } catch (Exception e) {
            log.error("Failed to fetch movie title for movieId: {}", movieId, e);
            return null;
        }
    }

    public List<MovieSummaryResponse> getAvailableMoviesForDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String url = MOVIE_SERVICE_URL + "/available-for-range?startDate=" + startDate + "&endDate=" + endDate;
            ResponseEntity<List<MovieSummaryResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<MovieSummaryResponse>>() {
                    });
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch available movies for date range: {} to {}", startDate, endDate, e);
            return Collections.emptyList();
        }
    }

    /**
     * Update movie status to NOW_PLAYING when showtime is created
     */
    public void updateMovieToNowPlaying(UUID movieId) {
        try {
            String url = MOVIE_SERVICE_URL + "/" + movieId + "/set-now-playing";
            restTemplate.postForObject(url, null, Void.class);
            log.info("Updated movie {} status to NOW_PLAYING", movieId);
        } catch (Exception e) {
            log.error("Failed to update movie {} status to NOW_PLAYING", movieId, e);
        }
    }
}
