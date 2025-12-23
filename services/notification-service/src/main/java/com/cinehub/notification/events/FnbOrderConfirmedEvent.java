package com.cinehub.notification.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FnbOrderConfirmedEvent(
        UUID orderId,
        UUID userId,
        String orderCode,
        UUID theaterId,
        BigDecimal totalAmount,
        List<FnbItemDetail> items,
        String language) {

    public record FnbItemDetail(
            String itemName,
            String itemNameEn,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice) {
    }
}
