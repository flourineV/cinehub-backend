package com.cinehub.fnb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FnbItemResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal unitPrice;
}