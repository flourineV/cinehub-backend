package com.cinehub.notification.events.dto;

import java.math.BigDecimal;

public record FnbDetail(
        String itemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {
}
