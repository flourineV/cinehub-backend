package com.cinehub.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 🧩 Exchange của Showtime (Booking sẽ CONSUME từ đây)
    public static final String BOOKING_SEAT_EVENTS_QUEUE = "booking.seat.events.queue";
    public static final String SHOWTIME_EXCHANGE = "showtime.exchange";
    public static final String SEAT_LOCK_ROUTING_KEY = "seat.locked";
    public static final String SEAT_UNLOCK_ROUTING_KEY = "seat.unlocked";

    // === EXCHANGE NHẬN TỪ PAYMENT ===
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_SUCCESS_KEY = "key.payment.success";
    public static final String PAYMENT_FAILED_KEY = "key.payment.failed";

    // 🧩 Exchange riêng của Booking (Booking sẽ PUBLISH ra)
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_CREATED_KEY = "booking.created";
    public static final String BOOKING_CONFIRMED_KEY = "key.booking.confirmed"; // gửi sang Showtime
    public static final String BOOKING_CANCELLED_KEY = "key.booking.cancelled"; // gửi sang Showtime

    // 1. Queue của Booking (Nếu Booking là Consumer)
    @Bean
    public Queue bookingQueue() {
        return new Queue(BOOKING_SEAT_EVENTS_QUEUE, true);
    }

    // === EXCHANGES ===
    @Bean
    public DirectExchange showtimeExchange() {
        return new DirectExchange(SHOWTIME_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange bookingExchange() {
        return new DirectExchange(BOOKING_EXCHANGE, true, false);
    }

    // === BINDINGS (Booking consume từ cả 2 exchange: Showtime + Payment) ===
    @Bean
    public Binding seatLockedBinding(Queue bookingSeatEventsQueue, DirectExchange showtimeExchange) {
        return BindingBuilder.bind(bookingSeatEventsQueue)
                .to(showtimeExchange)
                .with(SEAT_LOCK_ROUTING_KEY);
    }

    @Bean
    public Binding seatUnlockedBinding(Queue bookingSeatEventsQueue, DirectExchange showtimeExchange) {
        return BindingBuilder.bind(bookingSeatEventsQueue)
                .to(showtimeExchange)
                .with(SEAT_UNLOCK_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue bookingQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(bookingQueue)
                .to(paymentExchange)
                .with(PAYMENT_SUCCESS_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue bookingQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(bookingQueue)
                .to(paymentExchange)
                .with(PAYMENT_FAILED_KEY);
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
