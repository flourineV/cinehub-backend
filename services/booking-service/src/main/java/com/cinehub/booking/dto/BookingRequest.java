package com.cinehub.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingRequest {
    private UUID userId;
    private UUID showtimeId;
    private List<UUID> seatIds;
}
