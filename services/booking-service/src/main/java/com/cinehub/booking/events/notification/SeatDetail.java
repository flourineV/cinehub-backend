package com.cinehub.booking.events.notification;

import java.math.BigDecimal;

public record SeatDetail(
        String seatName,
        String seatType,
        String ticketType,
        int quantity,
        BigDecimal price) {
}
