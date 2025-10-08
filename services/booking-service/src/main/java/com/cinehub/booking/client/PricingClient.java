package com.cinehub.booking.client;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PricingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "http://localhost:8087/api"; // URL của pricing-service

    public SeatPriceResponse getSeatPrice(String seatType) {
        String url = BASE_URL + "/prices/seats/" + seatType;
        return restTemplate.getForObject(url, SeatPriceResponse.class);
    }

    public List<ComboResponse> getCombos(List<UUID> comboIds) {
        return comboIds.stream()
                .map(id -> restTemplate.getForObject(BASE_URL + "/combos/" + id, ComboResponse.class))
                .collect(Collectors.toList());
    }

    public List<PromotionResponse> getPromotions(List<UUID> promoIds) {
        return promoIds.stream()
                .map(id -> restTemplate.getForObject(BASE_URL + "/promotions/" + id, PromotionResponse.class))
                .collect(Collectors.toList());
    }
}
