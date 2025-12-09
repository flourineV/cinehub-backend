package com.cinehub.promotion.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromotionNotificationRequest {
    private List<String> userEmails;
    private String promotionCode;
    private String promotionType;
    private String discountType;
    private BigDecimal discountValue;
    private String discountValueDisplay;
    private String description;
    private String promoDisplayUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String validUntil;
    private String usageRestriction;
    private String actionUrl;
}
