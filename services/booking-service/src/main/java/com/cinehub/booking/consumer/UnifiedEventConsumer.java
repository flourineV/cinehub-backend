package com.cinehub.booking.consumer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.events.payment.PaymentCompletedEvent;
import com.cinehub.booking.events.payment.PaymentFailedEvent;
import com.cinehub.booking.events.showtime.SeatLockedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header; // ✅ Import Header
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

// booking-service/consumer/UnifiedEventConsumer.java (Ví dụ)

@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingService bookingService;

    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE)
    public void consume(
            @Payload Map<String, Object> rawMessage,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("📥 Received unified event | RoutingKey: {}", routingKey);
        Object dataObj = rawMessage.get("data");

        if (dataObj == null) {
            log.warn("Payload 'data' is missing for RoutingKey: {}", routingKey);
            return;
        }

        try {
            switch (routingKey) {
                // LOGIC SHOWTIME
                case RabbitConfig.SEAT_LOCK_ROUTING_KEY -> {
                    SeatLockedEvent data = objectMapper.convertValue(dataObj, SeatLockedEvent.class);
                    log.info("🔓 Seatlocked received: {}", data);
                    bookingService.handleSeatLocked(data); // TẠO BOOKING PENDING
                }
                case RabbitConfig.SEAT_UNLOCK_ROUTING_KEY -> {
                    SeatUnlockedEvent data = objectMapper.convertValue(dataObj, SeatUnlockedEvent.class);
                    log.info("🔓 SeatUnlocked received: {}", data);
                    bookingService.handleSeatUnlocked(data);
                }

                // LOGIC PAYMENT
                case RabbitConfig.PAYMENT_SUCCESS_KEY -> {
                    PaymentCompletedEvent data = objectMapper.convertValue(dataObj, PaymentCompletedEvent.class);
                    log.info("Processing PaymentSuccess for booking {}", data.bookingId());
                    bookingService.updateBookingStatus(data.bookingId(), BookingStatus.CONFIRMED);
                }
                case RabbitConfig.PAYMENT_FAILED_KEY -> {
                    PaymentFailedEvent data = objectMapper.convertValue(dataObj, PaymentFailedEvent.class);
                    log.info("Processing PaymentFailed for booking {}", data.bookingId());
                    bookingService.updateBookingStatus(data.bookingId(), BookingStatus.CANCELLED);
                }
                default -> {
                    log.warn("⚠️ Unknown routing key: {}", routingKey);
                }
            }
        } catch (Exception e) {
            log.error("❌ Error processing event (RK: {}): {}", routingKey, e.getMessage(), e);
            throw new RuntimeException("Error processing event.", e);
        }
    }
}