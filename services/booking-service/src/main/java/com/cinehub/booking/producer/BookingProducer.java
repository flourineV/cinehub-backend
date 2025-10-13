package com.cinehub.booking.producer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 📤 Gửi event khi booking được tạo → PaymentService xử lý thanh toán
     */
    public void sendBookingCreatedEvent(BookingCreatedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_CONFIRMED_KEY;

        log.info("📤 Sending BookingCreatedEvent → PaymentService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, event.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    /**
     * 📤 Gửi event khi booking được CONFIRMED hoặc CANCELLED → ShowtimeService cập
     * nhật ghế
     */
    public void sendBookingStatusUpdatedEvent(BookingStatusUpdatedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = switch (event.status()) {
            case "CONFIRMED" -> "key.booking.confirmed";
            case "CANCELLED" -> "key.booking.cancelled";
            default -> "key.booking.unknown";
        };

        log.info(
                "📤 Sending BookingStatusUpdatedEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}, status={}",
                EXCHANGE, ROUTING_KEY, event.bookingId(), event.status());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
