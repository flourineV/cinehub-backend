package com.cinehub.booking.events.booking;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 📦 Event gửi sang PaymentService sau khi Booking được finalize hoàn tất.
 * PaymentService sẽ nhận event này để cập nhật lại số tiền (amount) trước khi
 * thanh toán.
 */
public record BookingFinalizedEvent(
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        BigDecimal finalPrice) {
}
