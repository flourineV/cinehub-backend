package com.cinehub.booking.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor

public class SeatLockClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${showtime.service.url:http://localhost:8085/api/seat-lock}")
    private String baseUrl;

    public SeatLockResponse lockSeat(UUID showtimeId, UUID seatId, UUID bookingId) {
        String url = baseUrl + "/lock";

        Map<String, UUID> body = new HashMap<>();
        body.put("showtimeId", showtimeId);
        body.put("seatId", seatId);
        body.put("bookingId", bookingId);

        return restTemplate.postForObject(url, body, SeatLockResponse.class);
    }
}
