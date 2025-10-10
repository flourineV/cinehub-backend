package com.cinehub.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // === Nhận từ Booking ===
    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String BOOKING_EVENT_EXCHANGE = "booking.event.exchange";
    public static final String BOOKING_SUCCESS_ROUTING_KEY = "key.booking.success";
    public static final String BOOKING_FAILED_ROUTING_KEY = "key.booking.failed";

    // === Gửi đi (tới Booking & Notification) ===
    public static final String PAYMENT_EVENT_EXCHANGE = "payment.event.exchange";
    public static final String PAYMENT_SUCCESS_KEY = "key.payment.completed";
    public static final String PAYMENT_FAILED_KEY = "key.payment.failed";
    public static final String NOTIFICATION_ROUTING_KEY = "key.notification.ready";

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public DirectExchange bookingEventExchange() {
        return new DirectExchange(BOOKING_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentEventExchange() {
        return new DirectExchange(PAYMENT_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding bookingToPaymentBinding(Queue paymentQueue, DirectExchange bookingEventExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(bookingEventExchange)
                .with(BOOKING_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
