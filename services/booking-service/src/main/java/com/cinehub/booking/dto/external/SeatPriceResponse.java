package com.cinehub.booking.dto.external;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeatPriceResponse {
    private String seatType;
    private BigDecimal basePrice;
}
