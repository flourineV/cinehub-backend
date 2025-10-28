package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Gửi sang Booking khi thanh toán thành công.
 */
public record PaymentSuccessEvent(
        UUID paymentId,
        UUID bookingId,
        UUID showtimeId,
        UUID userId,
        BigDecimal amount,
        String method,
        List<UUID> seatIds,
        String message) {
}
