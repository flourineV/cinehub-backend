package com.cinehub.pricing.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatPriceRequest {
    private String seatType;
    private BigDecimal basePrice;
    private String currency;
}
