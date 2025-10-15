package com.cinehub.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeatResponse {
    private UUID seatId;

    // ✅ THÊM: Loại ghế (NORMAL/VIP)
    private String seatType;

    // ✅ THÊM: Loại vé (ADULT/CHILD)
    private String ticketType;

    private BigDecimal price; // Giá của từng loại ghế/vé (Đã có trong entity)

    // ❌ XÓA: Trường 'status' (Thường được quản lý ở Booking cha)
    // private String status;
}