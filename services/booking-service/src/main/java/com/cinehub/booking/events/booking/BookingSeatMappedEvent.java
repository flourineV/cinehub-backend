package com.cinehub.booking.events.booking;

import java.util.List;
import java.util.UUID;

public record BookingSeatMappedEvent(
                UUID bookingId,
                UUID showtimeId,
                List<UUID> seatIds,
                UUID userId,
                String guestName,
                String guestEmail) {
}
