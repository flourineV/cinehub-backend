package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SeatLockRequest {
    private UUID showtimeId;
    private UUID seatId;
    private UUID bookingId;
}
