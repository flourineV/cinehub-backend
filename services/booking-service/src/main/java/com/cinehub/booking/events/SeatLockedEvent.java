package com.cinehub.booking.events;

import java.util.List;
import java.util.UUID;

public record SeatLockedEvent(
                UUID userId,
                UUID showtimeId,
                List<UUID> seatIds,
                List<String> seatTypes,
                int ttlSeconds) {
}
