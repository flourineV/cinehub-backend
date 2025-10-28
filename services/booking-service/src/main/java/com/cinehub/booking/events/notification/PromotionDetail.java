package com.cinehub.booking.events.notification;

import java.math.BigDecimal;

public record PromotionDetail(
        String code,
        BigDecimal discountAmount) {
}
