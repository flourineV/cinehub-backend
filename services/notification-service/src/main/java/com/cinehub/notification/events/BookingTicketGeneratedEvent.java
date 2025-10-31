package com.cinehub.notification.events;

import com.cinehub.notification.events.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingTicketGeneratedEvent(
        UUID bookingId,
        UUID userId,
        String movieTitle,
        String cinemaName,
        String roomName,
        String showDateTime, // "15/02/2025 20:45"
        List<SeatDetail> seats,
        List<FnbDetail> fnbs,
        PromotionDetail promotion,
        BigDecimal totalPrice,
        String rankName,
        BigDecimal rankDiscountAmount,  
        BigDecimal finalPrice,
        String paymentMethod,
        LocalDateTime createdAt) {
}
