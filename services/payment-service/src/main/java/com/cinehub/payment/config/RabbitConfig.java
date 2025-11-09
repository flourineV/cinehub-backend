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

    // showtime exchange
    public static final String SHOWTIME_EXCHANGE = "showtime.exchange";

    // routing key from payment queue to connect booking exchange
    public static final String BOOKING_CREATED_KEY = "booking.created";
    public static final String BOOKING_FINALIZED_KEY = "booking.finalized";
    public static final String BOOKING_REFUNDED_KEY = "booking.refunded";

    // routing key from payment queue to connect showtime exchange
    public static final String SEAT_UNLOCK_ROUTING_KEY = "seat.unlocked";

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

    @Bean
    public DirectExchange bookingExchange() {
        // Khai báo Exchange này để tạo Binding, nhưng nó thuộc về Booking Service
        return new DirectExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange showtimeExchange() {
        return new DirectExchange(SHOWTIME_EXCHANGE, true, false);
    }

    @Bean
    public Binding bookingToPaymentBinding(Queue paymentQueue, DirectExchange bookingExchange) {
        // Payment chỉ cần lắng nghe BookingCreated để bắt đầu giao dịch
        return BindingBuilder.bind(paymentQueue)
                .to(bookingExchange)
                .with(BOOKING_CREATED_KEY);
    }

    @Bean
    public Binding bookingFinalizedBinding(Queue paymentQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(bookingExchange)
                .with(BOOKING_FINALIZED_KEY);
    }

    @Bean
    public Binding seatUnlockedBinding(Queue paymentQueue, DirectExchange showtimeExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(showtimeExchange)
                .with(SEAT_UNLOCK_ROUTING_KEY);
    }

    @Bean
    public Binding bookingRefundedBinding(Queue paymentQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(bookingExchange)
                .with(BOOKING_REFUNDED_KEY);
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