package com.cinehub.payment.producer;

import com.cinehub.payment.config.RabbitConfig;
import com.cinehub.payment.events.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

        private final RabbitTemplate rabbitTemplate;

        public void sendPaymentSuccessEvent(Object event) {
                rabbitTemplate.convertAndSend(
                                RabbitConfig.PAYMENT_EVENT_EXCHANGE,
                                RabbitConfig.PAYMENT_SUCCESS_KEY,
                                event);
                log.info("📤 Sent PaymentCompletedEvent to Booking & Notification");
        }

        public void sendPaymentFailedEvent(Object event) {
                rabbitTemplate.convertAndSend(
                                RabbitConfig.PAYMENT_EVENT_EXCHANGE,
                                RabbitConfig.PAYMENT_FAILED_KEY,
                                event);
                log.info("📤 Sent PaymentFailedEvent to Booking");
        }
}
