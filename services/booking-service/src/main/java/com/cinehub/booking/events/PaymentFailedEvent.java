package com.cinehub.booking.events; // hoặc com.cinehub.payment.events

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String method,
        String transactionRef,
        String reason // ví dụ: "Insufficient funds" hoặc "Timeout from gateway"
) {
}
