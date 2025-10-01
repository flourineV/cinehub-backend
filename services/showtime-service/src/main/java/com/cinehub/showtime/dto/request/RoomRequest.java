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

public class RoomRequest {
    private UUID theaterId;
    private String name;
    private int seatCount;
}
