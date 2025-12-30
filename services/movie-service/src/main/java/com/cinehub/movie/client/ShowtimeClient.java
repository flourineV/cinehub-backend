package com.cinehub.movie.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.showtime.url:http://localhost:8084}")
    private String showtimeServiceUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    public void suspendShowtimesByMovie(UUID movieId, String reason) {
        try {
            String url = showtimeServiceUrl + "/api/showtimes/internal/suspend-by-movie/" + movieId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("Content-Type", "application/json");
            
            SuspendRequest request = new SuspendRequest(reason);
            HttpEntity<SuspendRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully suspended showtimes for movie {}", movieId);
            } else {
                log.warn("Failed to suspend showtimes for movie {}: {}", movieId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error suspending showtimes for movie {}: {}", movieId, e.getMessage(), e);
            // Don't throw exception to avoid breaking the movie archiving process
        }
    }

    public static record SuspendRequest(String reason) {}
}