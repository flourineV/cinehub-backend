package com.cinehub.booking.producer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.booking.BookingFinalizedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;
import com.cinehub.booking.events.booking.BookingSeatMappedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.events.booking.EventMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 📤 Gửi event khi booking được tạo.
     * Routing Key: BOOKING_CREATED_KEY
     * Destination: Payment Service
     */
    public void sendBookingCreatedEvent(BookingCreatedEvent data) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_CREATED_KEY;

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "BookingCreated",
                "v1",
                Instant.now(),
                data);

        log.info("📤 Sending BookingCreatedEvent → PaymentService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, data.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }

    /**
     * 📤 Gửi event khi booking được CONFIRMED hoặc CANCELLED.
     * Routing Key: BOOKING_CONFIRMED_KEY hoặc BOOKING_CANCELLED_KEY
     * Destination: Showtime Service (để cập nhật ghế)
     */
    public void sendBookingStatusUpdatedEvent(BookingStatusUpdatedEvent data) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = switch (data.newStatus()) {
            case CONFIRMED -> RabbitConfig.BOOKING_CONFIRMED_KEY;
            case CANCELLED -> RabbitConfig.BOOKING_CANCELLED_KEY;
            default -> "key.booking.unknown";
        };

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "BookingStatusUpdated",
                "v1",
                Instant.now(),
                data);

        log.info(
                "📤 Sending BookingStatusUpdatedEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}, status={}",
                EXCHANGE, ROUTING_KEY, data.bookingId(), data.newStatus());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }

    /**
     * 📤 Gửi event khi booking hết hạn (timeout hoặc scheduler).
     * Routing Key: BOOKING_EXPIRED_KEY
     * Destination: Showtime Service (để giải phóng ghế)
     */
    public void sendBookingExpiredEvent(BookingStatusUpdatedEvent data) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_EXPIRED_KEY;

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "BookingExpired",
                "v1",
                Instant.now(),
                data);

        log.warn("📤 Sending BookingExpiredEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, data.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }

    public void sendBookingFinalizedEvent(BookingFinalizedEvent data) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_FINALIZED_KEY;

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "BookingFinalized",
                "v1",
                Instant.now(),
                data);

        log.info(
                "📤 Sending BookingFinalizedEvent → PaymentService | exchange={}, routingKey={}, bookingId={}, finalPrice={}",
                EXCHANGE, ROUTING_KEY, data.bookingId(), data.finalPrice());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }

    /**
     * 📤 Gửi event ánh xạ ghế (bookingId → seat lock mapping).
     * Routing Key: BOOKING_SEAT_MAPPED_KEY
     * Destination: Showtime Service
     */
    public void sendBookingSeatMappedEvent(BookingSeatMappedEvent data) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_SEAT_MAPPED_KEY;

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "BookingSeatMapped",
                "v1",
                Instant.now(),
                data);

        log.info("📤 Sending BookingSeatMappedEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, data.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }

    /**
     * 📤 Gửi event mở khoá ghế thủ công (thường do Payment thất bại).
     * Routing Key: BOOKING_SEAT_UNLOCK_KEY
     * Destination: Showtime Service
     */
    public void sendSeatUnlockedEvent(SeatUnlockedEvent data) {
        final String EXCHANGE = RabbitConfig.SHOWTIME_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_SEAT_UNLOCK_KEY;

        var msg = new EventMessage<>(
                UUID.randomUUID().toString(),
                "SeatUnlocked",
                "v1",
                Instant.now(),
                data);

        log.warn(
                "📤 Sending SeatUnlockedEvent (REQUEST) → ShowtimeService | exchange={}, routingKey={}, bookingId={}, reason={}",
                EXCHANGE, ROUTING_KEY, data.bookingId(), data.reason());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
    }
}
