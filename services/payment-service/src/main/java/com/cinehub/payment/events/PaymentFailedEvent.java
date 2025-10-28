package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Gửi sang Booking khi thanh toán thất bại.
 */
public record PaymentFailedEvent(
                UUID paymentId,
                UUID bookingId,
                UUID showtimeId,
                UUID userId,
                BigDecimal amount,
                String method,
                List<UUID> seatIds,
                String reason) {
}
