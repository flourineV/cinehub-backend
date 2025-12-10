package com.cinehub.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FnbOrderConfirmationRequest {
    private UUID userId;
    private String userEmail;
    private String userName;
    private String orderCode;
    private UUID theaterId;
    private BigDecimal totalAmount;
    private List<FnbItemDetail> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FnbItemDetail {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
