package com.cinehub.fnb.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEvent(
        UUID paymentId,
        UUID fnbOrderId,
        UUID userId,
        BigDecimal amount,
        String method,
        String message) {
}
