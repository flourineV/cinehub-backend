package com.cinehub.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceClient {

    private final RestTemplate restTemplate;
    private static final String SHOWTIME_SERVICE_URL = "http://showtime-service:8084/api/showtimes";

    @Value("${app.internal.secret-key}")
    private String internalSecretKey;

    /**
     * Extend seat lock TTL to 10 minutes when payment is initiated
     */
    public void extendSeatLockForPayment(UUID showtimeId, List<UUID> seatIds, UUID userId, UUID guestSessionId) {
        try {
            String url = SHOWTIME_SERVICE_URL + "/seat-lock/extend-for-payment";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalSecretKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("showtimeId", showtimeId);
            requestBody.put("seatIds", seatIds);
            if (userId != null) {
                requestBody.put("userId", userId);
            }
            if (guestSessionId != null) {
                requestBody.put("guestSessionId", guestSessionId);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("Extended seat lock for showtime {} - {} seats", showtimeId, seatIds.size());

        } catch (Exception e) {
            log.error("Failed to extend seat lock for showtime {}", showtimeId, e);
            throw new RuntimeException("Cannot extend seat lock. Please try again.", e);
        }
    }
}
