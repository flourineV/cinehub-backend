package com.cinehub.promotion.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class RefundVoucherRequest {
    private UUID userId;
    private BigDecimal value;
    private LocalDateTime expiredAt;
}
