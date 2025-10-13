package com.cinehub.showtime.events;

import com.cinehub.showtime.dto.request.SeatSelectionDetail; // Cần import DTO mới
import java.util.List;
import java.util.UUID;

public record SeatLockedEvent(
                UUID userId,
                UUID showtimeId,
                // ĐÃ SỬA: Thay thế seatIds và seatTypes bằng danh sách chi tiết
                List<SeatSelectionDetail> selectedSeats,
                int ttlSeconds) {
}