package com.cinehub.booking.events.showtime;

import com.cinehub.booking.dto.external.SeatSelectionDetail;
import java.util.List;
import java.util.UUID;

public record SeatLockedEvent(
        UUID userId,
        UUID showtimeId,
        List<SeatSelectionDetail> selectedSeats,
        int ttlSeconds) {
}