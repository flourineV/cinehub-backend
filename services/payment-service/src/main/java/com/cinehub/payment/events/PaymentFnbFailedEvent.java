package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFnbFailedEvent(
                UUID paymentId,
                UUID fnbOrderId,
                UUID userId,
                BigDecimal amount,
                String method,
                String message,
                String reason) {
}