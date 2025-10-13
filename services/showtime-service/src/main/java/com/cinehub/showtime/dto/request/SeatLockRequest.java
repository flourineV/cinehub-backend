package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockRequest {
    private UUID userId;
    private UUID showtimeId;

    // Đã thay đổi: Truyền list chi tiết các ghế đã chọn
    private List<SeatSelectionDetail> selectedSeats;
}