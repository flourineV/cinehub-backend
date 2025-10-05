package com.cinehub.profile.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankRequest {

    @NotBlank(message = "Rank name is required")
    private String name;

    @NotNull(message = "Minimum points required")
    private Integer minPoints;

    private Integer maxPoints;

    @DecimalMin(value = "0.00", message = "Discount rate must be >= 0")
    @DecimalMax(value = "100.00", message = "Discount rate must be <= 100")
    private BigDecimal discountRate;
}
