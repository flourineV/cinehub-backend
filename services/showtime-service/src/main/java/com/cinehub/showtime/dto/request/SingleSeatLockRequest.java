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
public class SingleSeatLockRequest {
    private UUID userId;
    private UUID guestSessionId;
    private UUID showtimeId;
    private SeatSelectionDetail selectedSeat;
}
