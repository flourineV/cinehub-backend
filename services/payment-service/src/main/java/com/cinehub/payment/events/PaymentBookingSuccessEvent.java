package com.cinehub.payment.events;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event gửi sang Booking Service khi thanh toán booking thành công.
 */
public record PaymentBookingSuccessEvent(
        UUID paymentId,
        UUID bookingId,
        UUID showtimeId,
        UUID userId,
        BigDecimal amount,
        String method,
        List<UUID> seatIds,
        String message) {
}