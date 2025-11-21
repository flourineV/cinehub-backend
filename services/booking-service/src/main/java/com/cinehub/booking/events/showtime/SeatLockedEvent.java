package com.cinehub.booking.events.showtime;

import com.cinehub.booking.dto.external.SeatSelectionDetail;
import java.util.List;
import java.util.UUID;

public record SeatLockedEvent(
        UUID userId,
        String guestName,
        String guestEmail,
        UUID showtimeId,
        List<SeatSelectionDetail> selectedSeats,
        int ttlSeconds) {
}