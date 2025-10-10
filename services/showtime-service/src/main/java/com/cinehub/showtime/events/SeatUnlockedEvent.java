package com.cinehub.showtime.events;

import java.util.List;
import java.util.UUID;

public record SeatUnlockedEvent(
                UUID showtimeId,
                List<UUID> seatIds,
                String reason // "timeout" , "cancelled", "system_released"
) {
}