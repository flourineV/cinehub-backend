package com.cinehub.fnb.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FnbOrderItemResponse {
    private UUID fnbItemId;
    private String itemName;
    private String itemNameEn;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
