package com.cinehub.booking.producer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.events.booking.BookingSeatMappedEvent;

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
     * 📤 Gửi event khi booking được tạo.
     * Routing Key: BOOKING_CREATED_KEY
     * Destination: Payment Service
     */
    public void sendBookingCreatedEvent(BookingCreatedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_CREATED_KEY;

        log.info("📤 Sending BookingCreatedEvent → PaymentService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, event.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    /**
     * 📤 Gửi event khi booking được CONFIRMED hoặc CANCELLED.
     * Routing Key: BOOKING_CONFIRMED_KEY hoặc BOOKING_CANCELLED_KEY
     * Destination: Showtime Service (để cập nhật ghế)
     */
    public void sendBookingStatusUpdatedEvent(BookingStatusUpdatedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = switch (event.newStatus()) {
            case CONFIRMED -> RabbitConfig.BOOKING_CONFIRMED_KEY;
            case CANCELLED -> RabbitConfig.BOOKING_CANCELLED_KEY;
            // Case EXPIRED sẽ được xử lý riêng hoặc được thêm vào đây tùy nhu cầu
            default -> "key.booking.unknown";
        };

        log.info(
                "📤 Sending BookingStatusUpdatedEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}, status={}",
                EXCHANGE, ROUTING_KEY, event.bookingId(), event.newStatus());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    /**
     * 📤 Gửi event khi booking hết hạn (dùng cho Scheduler nội bộ hoặc logic
     * timeout).
     * Routing Key: BOOKING_EXPIRED_KEY
     * Destination: Showtime Service (để giải phóng ghế)
     */
    public void sendBookingExpiredEvent(BookingStatusUpdatedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_EXPIRED_KEY;

        log.warn("📤 Sending BookingExpiredEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, event.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    /**
     * 📤 Gửi event ánh xạ giao dịch ghế (Giữ lại hàm này nếu bạn vẫn cần nó).
     * * LƯU Ý: SEAT_TRANSACTION_MAPPED_KEY KHÔNG có trong RabbitConfig, nên tôi
     * đang dùng giá trị mặc định.
     */
    public void sendBookingSeatMappedEvent(BookingSeatMappedEvent event) {
        final String EXCHANGE = RabbitConfig.BOOKING_EXCHANGE;
        // ⚠️ Lưu ý: SEAT_TRANSACTION_MAPPED_KEY KHÔNG được định nghĩa trong
        // RabbitConfig
        final String ROUTING_KEY = RabbitConfig.BOOKING_SEAT_MAPPED_KEY;

        log.info("📤 Sending SeatTransactionMappedEvent → ShowtimeService | exchange={}, routingKey={}, bookingId={}",
                EXCHANGE, ROUTING_KEY, event.bookingId());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }

    public void sendSeatUnlockedEvent(SeatUnlockedEvent event) {
        final String EXCHANGE = RabbitConfig.SHOWTIME_EXCHANGE;
        final String ROUTING_KEY = RabbitConfig.BOOKING_SEAT_UNLOCK_KEY;

        log.warn(
                "📤 Sending SeatUnlockedEvent (REQUEST) → ShowtimeService | exchange={}, routingKey={}, bookingId={}, reason={}",
                EXCHANGE, ROUTING_KEY, event.bookingId(), event.reason());

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}