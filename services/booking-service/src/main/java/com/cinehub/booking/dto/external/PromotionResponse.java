package com.cinehub.booking.dto.external;

import lombok.Data;
import java.util.UUID;

@Data
public class PromotionResponse {
    private UUID id;
    private String name;
    private double discountPercent;
    private boolean active;
}
