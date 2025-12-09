package com.cinehub.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatsResponse {
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long pendingPayments;
    private BigDecimal totalRevenue;
}
