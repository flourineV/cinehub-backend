package com.cinehub.booking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLoyaltyRequest {
    private Integer points;
    private UUID bookingId;
    private String bookingCode;
    private BigDecimal amountSpent;
    private String description;
}