package com.cinehub.booking.consumer;

import com.cinehub.booking.config.RabbitConfig;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.events.payment.PaymentCompletedEvent;
import com.cinehub.booking.events.payment.PaymentFailedEvent;
import com.cinehub.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header; // ✅ Import Header
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingService bookingService;

    // Lắng nghe các sự kiện Payment trên BOOKING_QUEUE
    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE) // ✅ Sửa tên Queue
    public void consume(
            @Payload Map<String, Object> rawMessage,
            @Header("amqp_receivedRoutingKey") String routingKey) { // ✅ Lấy Routing Key

        log.info("📥 Received payment event | RoutingKey: {}", routingKey);
        Object dataObj = rawMessage.get("data");

        if (dataObj == null) {
            log.warn("Payload 'data' is missing for RoutingKey: {}", routingKey);
            return;
        }

        try {
            switch (routingKey) { // ✅ Dùng Routing Key để phân loại
                case RabbitConfig.PAYMENT_SUCCESS_KEY -> { // Key: payment.success
                    PaymentCompletedEvent data = objectMapper.convertValue(dataObj,
                            PaymentCompletedEvent.class);
                    log.info("Processing PaymentSuccess for booking {}", data.bookingId());
                    // Cập nhật trạng thái CONFIRMED
                    bookingService.updateBookingStatus(data.bookingId(), BookingStatus.CONFIRMED);
                }
                case RabbitConfig.PAYMENT_FAILED_KEY -> { // Key: payment.failed
                    PaymentFailedEvent data = objectMapper.convertValue(dataObj,
                            PaymentFailedEvent.class);
                    log.info("Processing PaymentFailed for booking {}", data.bookingId());
                    // Cập nhật trạng thái CANCELLED
                    bookingService.updateBookingStatus(data.bookingId(), BookingStatus.CANCELLED);
                }
                default -> {
                    log.warn("Unknown routing key received by Payment consumer: {}", routingKey);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error processing Payment event (RK: {}): {}", routingKey, e.getMessage(), e);
            // Có thể ném RuntimeException để RabbitMQ Retry, tùy theo chính sách của bạn.
            throw new RuntimeException("Error processing payment event.", e);
        }
    }
}