// File: com.cinehub.fnb.dto.response.CalculatedFnbItemDto.java
package com.cinehub.fnb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CalculatedFnbItemDto {

    // Thông tin cơ bản từ request
    private UUID fnbItemId;
    private Integer quantity;

    // Giá đơn vị (Unit Price) được tính/tra cứu từ service FNB
    private BigDecimal unitPrice;

    // Tổng giá của mục này (quantity * unitPrice)
    private BigDecimal totalFnbItemPrice;
}