package com.cinehub.promotion.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundVoucherResponse {
    private UUID id;
    private String code;
    private UUID userId;
    private BigDecimal value;
    private Boolean isUsed;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
