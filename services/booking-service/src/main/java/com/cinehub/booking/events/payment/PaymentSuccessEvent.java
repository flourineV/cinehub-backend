package com.cinehub.booking.events.payment; // hoặc com.cinehub.payment.events, tùy service

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEvent(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String method,
        String transactionRef,
        String message) {
}
