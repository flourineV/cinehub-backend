package com.cinehub.notification.events.dto;

import java.math.BigDecimal;

public record PromotionDetail(
        String code,
        BigDecimal discountAmount) {
}
