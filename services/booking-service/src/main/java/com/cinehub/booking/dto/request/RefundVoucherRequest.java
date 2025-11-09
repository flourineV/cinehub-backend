package com.cinehub.booking.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundVoucherRequest {
    private UUID userId;
    private BigDecimal value;
    private LocalDateTime expiredAt;
}
