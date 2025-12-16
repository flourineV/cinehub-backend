package com.cinehub.profile.adapter.client;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingClient {

    private final RestTemplate restTemplate;

    @Value("${booking.service.url}")
    private String bookingServiceUrl;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

    public long getBookingCountByUserId(UUID userId) {
        try {
            String url = bookingServiceUrl + "/api/bookings/count?userId=" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Long> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Long.class);

            return response.getBody() != null ? response.getBody() : 0L;
        } catch (Exception e) {
            log.error("Failed to get booking count for userId {}: {}", userId, e.getMessage());
            return 0L;
        }
    }
}
