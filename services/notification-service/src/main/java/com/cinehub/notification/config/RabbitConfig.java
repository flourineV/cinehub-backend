package com.cinehub.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // notification queue
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    // from booking
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_TICKET_GENERATED_KEY = "booking.ticket.generated";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public DirectExchange bookingExchange() {
        return new DirectExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Binding bookingNotificationBinding(Queue notificationQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(bookingExchange)
                .with(BOOKING_TICKET_GENERATED_KEY);
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