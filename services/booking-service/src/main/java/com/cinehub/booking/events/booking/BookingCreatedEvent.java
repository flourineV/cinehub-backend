package com.cinehub.booking.events.booking;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookingCreatedEvent(
        UUID bookingId,
        UUID userId,
        String guestName,
        String guestEmail,
        UUID showtimeId,
        List<UUID> seatIds,
        BigDecimal totalPrice) {
}
