package com.cinehub.booking.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class BookingStatusRequest {

    // Thông tin cơ bản để tìm Booking (phải duy nhất)
    @NotNull
    private UUID userId;

    @NotNull
    private UUID showtimeId;

    // Client có thể gửi list các seatIds đã chọn để xác nhận
    // private List<UUID> seatIds;
}