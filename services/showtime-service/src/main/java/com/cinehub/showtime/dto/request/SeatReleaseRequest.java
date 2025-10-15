package com.cinehub.showtime.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatReleaseRequest {
    private UUID showtimeId;
    private List<UUID> seatIds; // Chỉ cần ID ghế
    private UUID bookingId; // ID giao dịch (có thể null nếu là Admin Release)
    private String reason; // Lý do: "manual_cancel", "admin_override", v.v.
}