package com.cinehub.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // showtime exchange
    public static final String SHOWTIME_EXCHANGE = "showtime.exchange";

    // routing key from booking queue to connect showtime exchange
    public static final String SEAT_UNLOCK_ROUTING_KEY = "seat.unlocked";

    // payment exchange
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // routing key from booking exchange
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    // booking exchange
    public static final String BOOKING_EXCHANGE = "booking.exchange";

    // routing key from booking exchange
    public static final String BOOKING_CREATED_KEY = "booking.created";
    public static final String BOOKING_CONFIRMED_KEY = "booking.confirmed";
    public static final String BOOKING_CANCELLED_KEY = "booking.cancelled";
    public static final String BOOKING_EXPIRED_KEY = "booking.expired";
    public static final String BOOKING_SEAT_UNLOCK_KEY = "seat.release.request";
    public static final String BOOKING_SEAT_MAPPED_KEY = "booking.seat.mapped";
    public static final String BOOKING_FINALIZED_KEY = "booking.finalized";
    public static final String BOOKING_REFUNDED_KEY = "booking.refunded";

    // notification
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String BOOKING_TICKET_GENERATED_KEY = "booking.ticket.generated";

    public static final String BOOKING_QUEUE = "booking.queue";

    @Bean
    public Queue bookingQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

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

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Binding seatUnlockedBinding(Queue bookingQueue, DirectExchange showtimeExchange) {
        return BindingBuilder.bind(bookingQueue)
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
