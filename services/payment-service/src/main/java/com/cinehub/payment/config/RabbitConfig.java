package com.cinehub.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // booking exchange
    public static final String BOOKING_EXCHANGE = "booking.exchange";

    // routing key from payment queue to connect booking exchange
    public static final String BOOKING_CREATED_KEY = "booking.created";

    // payment exchange
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // routing key from payment exchange
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    // payment queue
    public static final String PAYMENT_QUEUE = "payment.queue";

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    // === EXCHANGES ===

    // 1. Exchange của Booking (Dùng để Consume)
    @Bean
    public DirectExchange bookingExchange() {
        // Khai báo Exchange này để tạo Binding, nhưng nó thuộc về Booking Service
        return new DirectExchange(BOOKING_EXCHANGE, true, false);
    }

    // 2. Exchange của Payment (Dùng để Publish)
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    // === BINDINGS (Payment consume từ BOOKING_EXCHANGE) ===

    // Binding để nhận BookingCreatedEvent từ Booking Service
    @Bean
    public Binding bookingToPaymentBinding(Queue paymentQueue, DirectExchange bookingExchange) {
        // Payment chỉ cần lắng nghe BookingCreated để bắt đầu giao dịch
        return BindingBuilder.bind(paymentQueue)
                .to(bookingExchange)
                .with(BOOKING_CREATED_KEY);
    }

    // === CONFIG CHUNG ===

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