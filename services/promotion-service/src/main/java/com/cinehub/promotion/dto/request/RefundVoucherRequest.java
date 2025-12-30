package com.cinehub.promotion.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.cinehub.promotion.entity.RefundVoucher.RefundType;
import lombok.Data;

@Data
public class RefundVoucherRequest {
    private UUID userId;
    private BigDecimal value;
    private LocalDateTime expiredAt;
    private RefundType refundType; // USER_CANCELLED or SYSTEM_REFUND
}
