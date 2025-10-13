package com.cinehub.pricing.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

@Data
public class SeatPriceRequest {

    @NotBlank
    private String seatType;

    @NotBlank
    private String ticketType;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    private BigDecimal basePrice;

    private String description;
}