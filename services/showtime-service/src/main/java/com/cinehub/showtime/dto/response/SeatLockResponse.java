package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockResponse {
    private UUID showtimeId;
    private UUID seatId;
    private String status; // LOCKED / AVAILABLE / ALREADY_LOCKED
    private long ttl; // còn bao nhiêu giây thì hết hạn
}
