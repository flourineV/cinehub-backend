package com.cinehub.booking.client;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class ComboResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
}
