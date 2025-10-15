package com.cinehub.booking.events.booking;

import java.util.UUID;
import com.cinehub.booking.entity.BookingStatus;

public record BookingStatusUpdatedEvent(
        UUID bookingId,
        UUID showtimeId,
        UUID userId,
        BookingStatus newStatus,
        String previousStatus) {

}