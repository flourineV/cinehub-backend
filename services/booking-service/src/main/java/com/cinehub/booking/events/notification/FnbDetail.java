package com.cinehub.booking.events.notification;

import java.math.BigDecimal;

public record FnbDetail(
        String itemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {
}
