package com.cinehub.payment.dto.response;

import com.cinehub.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private UUID showtimeId;
    private List<UUID> seatIds;
    private BigDecimal amount;
    private String method;
    private PaymentStatus status;
    private String transactionRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
