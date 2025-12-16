package com.cinehub.booking.events.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
