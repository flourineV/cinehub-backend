package com.cinehub.showtime.events;

import java.util.List;
import java.util.UUID;

public record BookingSeatMappedEvent(
        UUID bookingId,
        UUID showtimeId,
        List<UUID> seatIds,
        UUID userId,
        UUID guestSessionId) {
}