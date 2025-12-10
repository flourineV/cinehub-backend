package com.cinehub.fnb.producer;

import com.cinehub.fnb.config.RabbitConfig;
import com.cinehub.fnb.events.EventMessage;
import com.cinehub.fnb.events.FnbOrderConfirmedEvent;
import com.cinehub.fnb.events.FnbOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FnbProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendFnbOrderCreatedEvent(FnbOrderCreatedEvent event) {
        try {
            EventMessage<FnbOrderCreatedEvent> message = new EventMessage<>("FNB_ORDER_CREATED", event);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.FNB_EXCHANGE,
                    RabbitConfig.FNB_ORDER_CREATED_KEY,
                    message);
            log.info("✅ Sent FnbOrderCreatedEvent | fnbOrderId={}", event.fnbOrderId());
        } catch (Exception e) {
            log.error("❌ Failed to send FnbOrderCreatedEvent: {}", e.getMessage(), e);
        }
    }

    public void sendFnbOrderConfirmedEvent(FnbOrderConfirmedEvent event) {
        try {
            EventMessage<FnbOrderConfirmedEvent> message = new EventMessage<>("FNB_ORDER_CONFIRMED", event);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.FNB_EXCHANGE,
                    RabbitConfig.FNB_ORDER_CONFIRMED_KEY,
                    message);
            log.info("✅ Sent FnbOrderConfirmedEvent | orderCode={}", event.orderCode());
        } catch (Exception e) {
            log.error("❌ Failed to send FnbOrderConfirmedEvent: {}", e.getMessage(), e);
        }
    }
}
