package com.cinehub.notification.consumer;

import com.cinehub.notification.config.RabbitConfig;
import com.cinehub.notification.events.BookingTicketGeneratedEvent;
import com.cinehub.notification.service.NotificationService;
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
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handleUnifiedEvents(
            @Payload Map<String, Object> raw,
            @Header("amqp_receivedRoutingKey") String routingKey
    ) {
        log.info("[NotificationConsumer] Received unified event | RoutingKey: {}", routingKey);

        try {
            Object dataObj = raw.get("data");

            switch (routingKey) {

                case RabbitConfig.BOOKING_TICKET_GENERATED_KEY -> {
                    BookingTicketGeneratedEvent event = objectMapper.convertValue(
                            dataObj,
                            BookingTicketGeneratedEvent.class
                    );
                    log.info("[NotificationConsumer] Processing BookingTicketGeneratedEvent | bookingId={}",
                            event.bookingId());
                    notificationService.sendSuccessBookingTicketNotification(event);
                }

                default -> log.warn("[NotificationConsumer] Received event with unknown RoutingKey: {}", routingKey);
            }

        } catch (Exception e) {
            log.error("[NotificationConsumer] ❌ Critical error during event processing for RK {}: {}", routingKey, e.getMessage(), e);
        }
    }
}
