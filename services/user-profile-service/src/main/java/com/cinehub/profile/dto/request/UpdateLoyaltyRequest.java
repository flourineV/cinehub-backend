package com.cinehub.profile.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateLoyaltyRequest {
    private Integer points;
    private UUID bookingId;
    private String bookingCode;
    private BigDecimal amountSpent;
    private String description;
}