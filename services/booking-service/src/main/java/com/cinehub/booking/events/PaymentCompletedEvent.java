package com.cinehub.booking.events; // hoặc com.cinehub.payment.events, tùy service

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String method,
        String transactionRef,
        String message // ví dụ: "Payment successful via VNPay"
) {
}
