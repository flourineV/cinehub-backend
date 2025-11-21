package com.cinehub.showtime.events;

import java.util.List;
import java.util.UUID;

public record SeatUnlockedEvent(
                UUID bookingId,
                UUID showtimeId,
                List<UUID> seatIds,
                String reason) {
}