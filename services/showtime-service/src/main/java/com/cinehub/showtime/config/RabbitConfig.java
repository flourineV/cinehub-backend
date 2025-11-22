package com.cinehub.showtime.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // showtime exchange
    public static final String SHOWTIME_EXCHANGE = "showtime.exchange";

    public static final String SEAT_UNLOCK_ROUTING_KEY = "seat.unlocked";

    public static final String SHOWTIME_QUEUE = "showtime.queue";

    public static final String BOOKING_EXCHANGE = "booking.exchange";

    public static final String BOOKING_CONFIRMED_KEY = "booking.confirmed";
    public static final String BOOKING_CANCELLED_KEY = "booking.cancelled";
    public static final String BOOKING_EXPIRED_KEY = "booking.expired";
    public static final String BOOKING_REFUNDED_KEY = "booking.refunded";
    public static final String SEAT_RELEASE_REQUEST_KEY = "seat.release.request";
    public static final String BOOKING_SEAT_MAPPED_KEY = "booking.seat.mapped";

    @Bean
    public DirectExchange showtimeExchange() {
        return new DirectExchange(SHOWTIME_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange bookingExchange() {
        return new DirectExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue showtimeQueue() {
        return new Queue(SHOWTIME_QUEUE, true);
    }

    @Bean
    public Binding bookingConfirmedBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(BOOKING_CONFIRMED_KEY);
    }

    @Bean
    public Binding bookingCancelledBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(BOOKING_CANCELLED_KEY);
    }

    @Bean
    public Binding bookingExpiredBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(BOOKING_EXPIRED_KEY);
    }

    @Bean
    public Binding bookingRefundedBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(BOOKING_REFUNDED_KEY);
    }

    @Bean
    public Binding seatReleaseRequestBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(SEAT_RELEASE_REQUEST_KEY);
    }

    @Bean
    public Binding bookingSeatMappedBinding(Queue showtimeQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(showtimeQueue)
                .to(bookingExchange)
                .with(BOOKING_SEAT_MAPPED_KEY);
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
