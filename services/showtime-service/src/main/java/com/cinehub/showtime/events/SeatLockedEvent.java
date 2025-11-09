package com.cinehub.showtime.events;

import com.cinehub.showtime.dto.request.SeatSelectionDetail; // Cần import DTO mới
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