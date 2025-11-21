package com.cinehub.booking.events.showtime;

import java.util.List;
import java.util.UUID;

public record SeatUnlockedEvent(
        UUID showtimeId,
        UUID bookingId,
        List<UUID> seatIds,
        String reason) {
}