package com.cinehub.movie.service.client;

import com.cinehub.movie.dto.TMDb.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class TMDbClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.url:https://api.themoviedb.org/3}")
    private String baseUrl;

    public List<TMDbMovieResponse> fetchNowPlaying() {
        String url = String.format("%s/movie/now_playing?api_key=%s&language=vi-VN&page=1", baseUrl, apiKey);
        TMDbMovieListResponse response = restTemplate.getForObject(url, TMDbMovieListResponse.class);

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults();
    }

    public List<TMDbMovieResponse> fetchUpcoming() {
        String url = String.format("%s/movie/upcoming?api_key=%s&language=vi-VN&page=1", baseUrl, apiKey);
        TMDbMovieListResponse response = restTemplate.getForObject(url, TMDbMovieListResponse.class);

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        return response.getResults();
    }

    public TMDbMovieResponse fetchMovieDetail(Integer tmdbId) {
        String url = String.format("%s/movie/%d?api_key=%s&language=vi-VN", baseUrl, tmdbId, apiKey);
        return restTemplate.getForObject(url, TMDbMovieResponse.class);
    }

    public TMDbCreditsResponse fetchCredits(Integer tmdbId) {
        String url = String.format("%s/movie/%d/credits?api_key=%s&language=vi-VN", baseUrl, tmdbId, apiKey);
        return restTemplate.getForObject(url, TMDbCreditsResponse.class);
    }

    public TMDbReleaseDatesResponse fetchReleaseDates(Integer tmdbId) {
        String url = String.format("%s/movie/%d/release_dates?api_key=%s", baseUrl, tmdbId, apiKey);
        return restTemplate.getForObject(url, TMDbReleaseDatesResponse.class);
    }

    public String fetchTrailerKey(Integer tmdbId) {
        String url = String.format("%s/movie/%d/videos?api_key=%s", baseUrl, tmdbId, apiKey);
        TMDbVideoResponse response = restTemplate.getForObject(url, TMDbVideoResponse.class);

        System.out.println("TMDb video response: " + response);

        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }

        // Ưu tiên official trailer, nếu không có thì lấy trailer bất kỳ
        return response.getResults().stream()
                .filter(v -> "Trailer".equalsIgnoreCase(v.getType()))
                .filter(v -> "YouTube".equalsIgnoreCase(v.getSite()))
                .filter(v -> v.getOfficial() != null ? v.getOfficial() : true)
                .map(v -> "https://www.youtube.com/watch?v=" + v.getKey())
                .findFirst()
                .orElse(
                        response.getResults().stream()
                                .filter(v -> "Trailer".equalsIgnoreCase(v.getType()))
                                .filter(v -> "YouTube".equalsIgnoreCase(v.getSite()))
                                .map(v -> "https://www.youtube.com/watch?v=" + v.getKey())
                                .findFirst()
                                .orElse(null));
    }
}
