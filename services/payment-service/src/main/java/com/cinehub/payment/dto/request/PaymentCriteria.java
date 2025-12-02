package com.cinehub.payment.dto.request;

import com.cinehub.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCriteria {
    private UUID userId;
    private UUID bookingId;
    private UUID showtimeId;
    private String transactionRef;
    private PaymentStatus status;
    private String method;

    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
