package com.cinehub.booking.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class BookingStatusResponse {

    // Booking ID nếu đã tạo thành công
    private UUID bookingId;

    // Trạng thái hiện tại (ví dụ: PENDING, CONFLICT, NOT_FOUND)
    private PollingStatus status;

    // Thông báo chi tiết (nếu có lỗi)
    private String message;

    // Thời gian ghế còn hiệu lực (nếu bookingId đã có)
    // private Long ttlRemainingSeconds;
}