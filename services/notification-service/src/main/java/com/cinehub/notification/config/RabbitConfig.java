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

    // payment exchange
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // routing key from notification queue to connect payment exchange
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(paymentExchange)
                .with(PAYMENT_SUCCESS_KEY);
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