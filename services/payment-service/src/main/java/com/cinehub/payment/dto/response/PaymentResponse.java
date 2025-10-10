package com.cinehub.payment.dto.response;

import com.cinehub.payment.entity.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String method;
    private PaymentStatus status;
    private String transactionRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
