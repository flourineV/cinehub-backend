package com.cinehub.booking.dto.external;

import com.cinehub.booking.entity.DiscountType;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PromotionValidationResponse {
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Boolean isOneTimeUse;
}