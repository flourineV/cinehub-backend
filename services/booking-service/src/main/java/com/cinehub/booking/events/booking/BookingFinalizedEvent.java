package com.cinehub.booking.events.booking;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingFinalizedEvent(
        UUID bookingId,
        UUID userId,
        String guestName,
        String guestEmail,
        UUID showtimeId,
        BigDecimal finalPrice) {
}
