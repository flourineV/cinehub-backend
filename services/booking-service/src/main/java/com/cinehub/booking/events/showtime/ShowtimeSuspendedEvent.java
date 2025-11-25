package com.cinehub.booking.events.showtime;

import java.util.List;
import java.util.UUID;

public record ShowtimeSuspendedEvent(
    UUID showtimeId,
    UUID movieId,
    List<UUID> affectedBookingIds,
    String reason
) {}
