package com.cinehub.booking.client;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SeatPriceResponse {
    private String seatType;
    private BigDecimal price;
}
