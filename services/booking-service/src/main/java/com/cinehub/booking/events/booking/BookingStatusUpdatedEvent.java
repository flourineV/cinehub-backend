package com.cinehub.booking.events.booking;

import java.util.UUID;
import java.util.List;

public record BookingStatusUpdatedEvent(
                UUID bookingId,
                UUID showtimeId,
                List<UUID> seatIds,
                String newStatus,
                String previousStatus) {

}