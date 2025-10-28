package com.cinehub.notification.consumer;

import com.cinehub.notification.config.RabbitConfig;
import com.cinehub.notification.events.PaymentSuccessEvent;
import com.cinehub.notification.events.BookingTicketGeneratedEvent;
import com.cinehub.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void consume(@Payload Map<String, Object> rawMessage) {
        try {
            String type = (String) rawMessage.get("type");
            Object dataObj = rawMessage.get("data");

            log.info("NotificationService received event: {}", type);

            switch (type) {
                // case "PaymentSuccess" -> {
                // PaymentSuccessEvent event = objectMapper.convertValue(dataObj,
                // PaymentSuccessEvent.class);
                // notificationService.handlePaymentSuccess(event);
                // }
                case "BookingTicketGenerated" -> {
                    BookingTicketGeneratedEvent event = objectMapper.convertValue(dataObj,
                            BookingTicketGeneratedEvent.class);
                    notificationService.sendSuccessBookingTicketNotification(event);
                }
                default -> log.warn("⚠️ Unknown event type: {}", type);
            }

        } catch (Exception e) {
            log.error("❌ Error parsing event in NotificationService: {}", e.getMessage(), e);
        }
    }
}
