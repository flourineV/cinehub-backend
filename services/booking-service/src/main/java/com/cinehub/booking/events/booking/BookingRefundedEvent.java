package com.cinehub.booking.events.booking;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingRefundedEvent(
        UUID bookingId,
        UUID userId,
        BigDecimal refundedValue) {
}
