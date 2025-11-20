package com.cinehub.showtime.dto.response;

import com.cinehub.showtime.entity.ShowtimeSeat.SeatStatus;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeatResponse {
    private UUID seatId;
    private String seatNumber;
    private String type; // NORMAL, VIP, COUPLE
    private SeatStatus status;
}
