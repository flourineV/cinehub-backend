package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class SeatResponse {
    private UUID id;
    private String seatNumber;
    private String rowLabel;
    private int columnIndex;
    private String type;
    private String roomName;
}
