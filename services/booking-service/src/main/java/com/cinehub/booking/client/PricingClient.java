package com.cinehub.booking.client;

import com.cinehub.booking.dto.external.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class PricingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PricingClient(RestTemplate restTemplate,
            @Value("${pricing.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // 🪑 Lấy giá ghế theo seatType
    public SeatPriceResponse getSeatPrice(String seatType) {
        String url = baseUrl + "/api/prices/seats/" + seatType;
        return restTemplate.getForObject(url, SeatPriceResponse.class);
    }

    // 🍿 Lấy combo theo danh sách id
    public List<ComboResponse> getCombos(List<UUID> comboIds) {
        List<ComboResponse> result = new ArrayList<>();
        for (UUID id : comboIds) {
            String url = baseUrl + "/api/pricing/combos/" + id;
            result.add(restTemplate.getForObject(url, ComboResponse.class));
        }
        return result;
    }

    // 🎁 Lấy danh sách khuyến mãi đang hoạt động
    public List<PromotionResponse> getActivePromotions() {
        String url = baseUrl + "/api/pricing/promotions/active";
        PromotionResponse[] promos = restTemplate.getForObject(url, PromotionResponse[].class);
        return Arrays.asList(Objects.requireNonNull(promos));
    }

    // 🎟️ Lấy khuyến mãi cụ thể theo id
    public PromotionResponse getPromotion(UUID id) {
        String url = baseUrl + "/api/pricing/promotions/" + id;
        return restTemplate.getForObject(url, PromotionResponse.class);
    }
}
