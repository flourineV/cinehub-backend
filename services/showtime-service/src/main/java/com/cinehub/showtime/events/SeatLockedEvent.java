package com.cinehub.showtime.events;

import com.cinehub.showtime.dto.request.SeatSelectionDetail;
import java.util.List;
import java.util.UUID;

public record SeatLockedEvent(
        UUID userId,
        UUID guestSessionId,
        UUID showtimeId,
        List<SeatSelectionDetail> selectedSeats,
        int ttlSeconds) {
}