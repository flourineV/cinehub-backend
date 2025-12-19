package com.cinehub.notification.events;

import com.cinehub.notification.events.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingTicketGeneratedEvent(
                UUID bookingId,
                String bookingCode,
                UUID userId,
                String guestName,
                String guestEmail,
                String movieTitle,
                String cinemaName,
                String roomName,
                String showDateTime,
                List<SeatDetail> seats,
                List<FnbDetail> fnbs,
                PromotionDetail promotion,
                BigDecimal totalPrice,
                String rankName,
                BigDecimal rankDiscountAmount,
                BigDecimal finalPrice,
                String paymentMethod,
                LocalDateTime createdAt,
                String language) {
}
