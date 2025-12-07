package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyHistoryResponse {
    
    private UUID id;
    private UUID bookingId;
    private String type;
    private Integer pointsChange;
    private Integer pointsBefore;
    private Integer pointsAfter;
    private BigDecimal amountSpent;
    private String description;
    private LocalDateTime createdAt;
}
