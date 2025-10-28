package com.cinehub.payment.producer;

import com.cinehub.payment.config.RabbitConfig;
import com.cinehub.payment.events.EventMessage;
import com.cinehub.payment.events.PaymentSuccessEvent; // Event mới
import com.cinehub.payment.events.PaymentFailedEvent;
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

        public void sendPaymentSuccessEvent(PaymentSuccessEvent data) {
                final String EXCHANGE = RabbitConfig.PAYMENT_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_SUCCESS_KEY;

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "PaymentSuccess",
                                "v1",
                                Instant.now(),
                                data);

                log.info("Sending PaymentSuccessEvent → BookingService | exchange={}, routingKey={}, bookingId={}",
                                EXCHANGE, ROUTING_KEY, data.bookingId());

                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }

        public void sendPaymentFailedEvent(PaymentFailedEvent data) {
                final String EXCHANGE = RabbitConfig.PAYMENT_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_FAILED_KEY;

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "PaymentFailed", // Loại Event
                                "v1",
                                Instant.now(),
                                data);

                log.error("Sending PaymentFailedEvent → BookingService | exchange={}, routingKey={}, bookingId={}",
                                EXCHANGE, ROUTING_KEY, data.bookingId());

                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }
}
