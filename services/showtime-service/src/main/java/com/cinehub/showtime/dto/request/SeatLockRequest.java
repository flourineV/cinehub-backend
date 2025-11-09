package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockRequest {
    private UUID userId;
    private String guestName;
    private String guestEmail;
    private UUID showtimeId;
    private List<SeatSelectionDetail> selectedSeats;
}