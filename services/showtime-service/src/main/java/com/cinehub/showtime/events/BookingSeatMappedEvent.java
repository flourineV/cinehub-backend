package com.cinehub.showtime.events;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.UUID;

// Sử dụng record (từ Java 16) hoặc @Value/@Data của Lombok
public record BookingSeatMappedEvent(
                UUID bookingId,
                UUID showtimeId,
                List<UUID> seatIds,
                UUID userId // Giữ lại userId để kiểm tra tính toàn vẹn (tùy chọn)
) {
}