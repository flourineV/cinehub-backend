package com.cinehub.pricing.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatPriceResponse {
    private String seatType;
    private BigDecimal basePrice;
    private String currency;
}
