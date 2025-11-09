package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingRefundedEvent(
        UUID bookingId,
        UUID userId,
        BigDecimal refundedValue) {
}
