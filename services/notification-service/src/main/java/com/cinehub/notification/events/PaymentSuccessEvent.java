package com.cinehub.notification.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEvent(
                UUID paymentId,
                UUID bookingId,
                UUID userId,
                BigDecimal amount,
                String method,
                String message) {
}
