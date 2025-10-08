package com.cinehub.booking.client;

import java.util.UUID;

import lombok.Data;

@Data
public class PromotionResponse {
    private UUID id;
    private String name;
    private double discountPercent; // ví dụ 10.0 = 10%
}
