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

public class RoomResponse {
    private UUID id;
    private String name;
    private int seatCount;
    private String theaterName;
}
