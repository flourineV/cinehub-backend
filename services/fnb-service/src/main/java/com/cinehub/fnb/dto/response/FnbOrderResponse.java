package com.cinehub.fnb.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private UUID theaterId;
    private String orderCode;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime expiresAt; // PENDING orders expire after 5 minutes

    private List<FnbOrderItemResponse> items;
}
