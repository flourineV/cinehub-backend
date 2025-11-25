package com.cinehub.notification.events;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingRefundedEvent(
        UUID bookingId,
        UUID userId,
        String guestName,
        String guestEmail,
        UUID showtimeId,
        BigDecimal refundedValue,
        String refundMethod, String reason) {
}