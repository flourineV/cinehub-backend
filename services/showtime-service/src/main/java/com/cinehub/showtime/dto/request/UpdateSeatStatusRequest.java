package com.cinehub.showtime.dto.request;

import com.cinehub.showtime.entity.ShowtimeSeat.SeatStatus;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeatStatusRequest {
    private UUID showtimeId;
    private UUID seatId;
    private SeatStatus status;
}
