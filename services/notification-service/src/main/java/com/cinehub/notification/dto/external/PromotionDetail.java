package com.cinehub.notification.dto.external;

import java.math.BigDecimal;

public record PromotionDetail(
                String code,
                BigDecimal discountAmount) {
}
