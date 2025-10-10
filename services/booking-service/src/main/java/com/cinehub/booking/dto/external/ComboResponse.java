package com.cinehub.booking.dto.external;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ComboResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
}
