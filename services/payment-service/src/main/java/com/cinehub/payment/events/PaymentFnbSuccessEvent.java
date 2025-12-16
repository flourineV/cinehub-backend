package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFnbSuccessEvent(
                UUID paymentId,
                UUID fnbOrderId,
                UUID userId,
                BigDecimal amount,
                String method,
                String message) {
}