package com.cinehub.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingSeatResponse {
    private UUID seatId;
    private BigDecimal price;
    private String status; // RESERVED,CONFIRMED,CANCELLED
}
