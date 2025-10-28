package com.cinehub.showtime.events;

import java.util.UUID;
import java.util.List;

public record BookingStatusUpdatedEvent(
        UUID bookingId,
        UUID showtimeId,
        UUID userId,
        List<UUID> seatIds,
        String newStatus,
        String previousStatus) {

}