package com.cinehub.fnb.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FnbOrderResponse {
    private UUID id;
    private UUID userId;
    private String orderCode;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private List<FnbOrderItemResponse> items;
}
