package com.cinehub.promotion.dto.response;

import com.cinehub.promotion.entity.DiscountType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PromotionResponse {
    private UUID id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean isOneTimeUse;
    private String description;
}