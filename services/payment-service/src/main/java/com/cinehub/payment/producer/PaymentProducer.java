package com.cinehub.payment.producer;

import com.cinehub.payment.config.RabbitConfig;
import com.cinehub.payment.events.EventMessage;
import com.cinehub.payment.events.PaymentBookingSuccessEvent;
import com.cinehub.payment.events.PaymentFnbSuccessEvent;
import com.cinehub.payment.events.PaymentBookingFailedEvent;
import com.cinehub.payment.events.PaymentFnbFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

        private final RabbitTemplate rabbitTemplate;

        public void sendPaymentBookingSuccessEvent(PaymentBookingSuccessEvent data) {
                final String EXCHANGE = RabbitConfig.PAYMENT_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_BOOKING_SUCCESS_KEY;

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "PaymentBookingSuccess",
                                "v1",
                                Instant.now(),
                                data);

                log.info("Sending PaymentBookingSuccessEvent → BookingService | exchange={}, routingKey={}, bookingId={}",
                                EXCHANGE, ROUTING_KEY, data.bookingId());

                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }

        public void sendPaymentBookingFailedEvent(PaymentBookingFailedEvent data) {
                final String EXCHANGE = RabbitConfig.PAYMENT_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_BOOKING_FAILED_KEY;

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

        public void sendPaymentFnbSuccessEvent(PaymentFnbSuccessEvent data) {
                final String EXCHANGE = RabbitConfig.FNB_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_FNB_SUCCESS_KEY;

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "PaymentFnbSuccess",
                                "v1",
                                Instant.now(),
                                data);

                log.info("Sending PaymentFnbSuccessEvent → FnbService | fnbOrderId={}", data.fnbOrderId());
                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }

        public void sendPaymentFnbFailedEvent(PaymentFnbFailedEvent data) {
                final String EXCHANGE = RabbitConfig.FNB_EXCHANGE;
                final String ROUTING_KEY = RabbitConfig.PAYMENT_FNB_FAILED_KEY;

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "PaymentFnbFailed",
                                "v1",
                                Instant.now(),
                                data);

                log.info("Sending PaymentFnbFailedEvent → FnbService | fnbOrderId={}", data.fnbOrderId());
                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, msg);
        }
}
