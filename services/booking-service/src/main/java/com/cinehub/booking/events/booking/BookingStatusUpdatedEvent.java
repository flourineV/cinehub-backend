package com.cinehub.booking.events.booking;

import java.util.List;
import java.util.UUID;

public record BookingStatusUpdatedEvent(
                UUID bookingId,
                UUID showtimeId,
                List<UUID> seatIds,
                String status // CONFIRMED / CANCELLED
) {
}
