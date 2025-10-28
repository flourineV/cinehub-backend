package com.cinehub.notification.dto.external;

import java.math.BigDecimal;

public record SeatDetail(
                String seatName,
                String seatType,
                String ticketType,
                int quantity,
                BigDecimal price) {
}
