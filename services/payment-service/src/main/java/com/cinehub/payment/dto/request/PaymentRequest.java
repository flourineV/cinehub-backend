package com.cinehub.payment.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String method; // ví dụ: "VNPAY", "MOMO", "CASH"
}
