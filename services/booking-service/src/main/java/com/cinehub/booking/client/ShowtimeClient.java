package com.cinehub.booking.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShowtimeClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${showtime.service.url:http://localhost:8085/api/showtimes}")
    private String showtimeBaseUrl;

    public ShowtimeResponse getShowtimeById(UUID showtimeId) {
        String url = showtimeBaseUrl + "/" + showtimeId;
        return restTemplate.getForObject(url, ShowtimeResponse.class);
    }
}
