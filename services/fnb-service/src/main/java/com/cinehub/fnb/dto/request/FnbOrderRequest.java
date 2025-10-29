package com.cinehub.fnb.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class FnbOrderRequest {
    private UUID userId;
    private String paymentMethod;
    private List<FnbOrderItemRequest> items;

    @Data
    public static class FnbOrderItemRequest {
        private UUID fnbItemId;
        private int quantity;
    }
}
