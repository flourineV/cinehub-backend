package com.cinehub.booking.producer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.booking.BookingFinalizedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;
import com.cinehub.booking.events.booking.BookingSeatMappedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.events.booking.EventMessage;
import com.cinehub.booking.events.notification.BookingTicketGeneratedEvent;

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

        public void sendBookingStatusUpdatedEvent(BookingStatusUpdatedEvent data) {
                final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
                final String ROUTING_KEY = switch (data.newStatus().toString()) {
                        case "CONFIRMED" -> RabbitConfig.BOOKING_CONFIRMED_KEY;
                        case "CANCELLED" -> RabbitConfig.BOOKING_CANCELLED_KEY;
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

        public void sendBookingTicketGeneratedEvent(BookingTicketGeneratedEvent data) {
                final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
                final String ROUTING_KEY = "booking.ticket.generated"; // key thống nhất với noti-service

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "BookingTicketGenerated",
                                "v1",
                                Instant.now(),
                                data);

                log.info("🎟️ Sending BookingTicketGeneratedEvent → NotificationService | exchange={}, routingKey={}, bookingId={}",
                                EXCHANGE, ROUTING_KEY, data.bookingId());

                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }
}
