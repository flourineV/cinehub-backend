package com.cinehub.payment.consumer;

import com.cinehub.payment.config.RabbitConfig;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.events.BookingFinalizedEvent;
import com.cinehub.payment.service.PaymentService; // Inject Service thay vì Producer
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService; // ✅ Inject PaymentService

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void handleUnifiedEvents(
            @Payload Map<String, Object> raw,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("📥 [PaymentConsumer] Received unified event | RoutingKey: {}", routingKey);

        try {
            Object dataObj = raw.get("data");

            switch (routingKey) {
                case RabbitConfig.BOOKING_CREATED_KEY -> {
                    BookingCreatedEvent event = objectMapper.convertValue(dataObj, BookingCreatedEvent.class);
                    log.info("📥 [PaymentConsumer] Processing BookingCreatedEvent | bookingId={}", event.bookingId());
                    paymentService.createPendingTransaction(event);
                }

                case RabbitConfig.BOOKING_FINALIZED_KEY -> {
                    BookingFinalizedEvent event = objectMapper.convertValue(dataObj, BookingFinalizedEvent.class);
                    log.info("📥 [PaymentConsumer] Processing BookingFinalizedEvent | bookingId={} | finalPrice={}",
                            event.bookingId(), event.finalPrice());
                    paymentService.updateFinalAmount(event);
                }

                default -> log.warn("⚠️ Received event with unknown Routing Key: {}", routingKey);
            }

        } catch (Exception e) {
            log.error("❌ Critical error during event processing for RK {}: {}", routingKey, e.getMessage(), e);
        }
    }

}