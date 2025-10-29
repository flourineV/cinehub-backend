package com.cinehub.profile.dto.response;

import java.util.UUID;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RankAndDiscountResponse {
    private UUID userId;
    private String rankName;
    private BigDecimal discountRate;
}
