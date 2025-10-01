package com.cinehub.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingSeatResponse {
    private String seatId;
    private BigDecimal price;
    private String status; // RESERVED,CONFIRMED,CANCELLED
}
