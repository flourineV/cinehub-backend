package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookingCreatedEvent(
                UUID bookingId,
                UUID userId,
                UUID showtimeId,
                List<UUID> seatIds,
                BigDecimal totalPrice) {
}
