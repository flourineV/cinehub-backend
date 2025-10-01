package com.cinehub.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingRequest {
    private String userId;
    private String showtimeId;
    private List<String> seatIds;
}
