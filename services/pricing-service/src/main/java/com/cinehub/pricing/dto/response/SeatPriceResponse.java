package com.cinehub.pricing.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SeatPriceResponse {

    private String seatType;
    private String ticketType;
    private BigDecimal basePrice;
}