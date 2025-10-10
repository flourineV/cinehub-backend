package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class SeatRequest {
    private UUID roomId;
    private String seatNumber; // A1, B2,
    private String rowLabel; // A, B, C
    private int columnIndex; // 1,2,3
    private String type; // NORMAL, VIP, COUPLE
}
