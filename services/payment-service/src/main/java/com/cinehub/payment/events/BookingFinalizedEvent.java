package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingFinalizedEvent(
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        BigDecimal finalPrice) {
}
