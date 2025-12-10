package com.cinehub.fnb.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String FNB_EXCHANGE = "fnb.exchange";
    public static final String FNB_ORDER_CREATED_KEY = "fnb.order.created";
    public static final String FNB_ORDER_CONFIRMED_KEY = "fnb.order.confirmed";
    public static final String PAYMENT_FNB_SUCCESS_KEY = "payment.fnb.success";

    // Queue for Payment Service to listen
    public static final String PAYMENT_QUEUE = "payment.queue";

    // Queue for FnB Service to listen payment events
    public static final String FNB_QUEUE = "fnb.queue";

    @Bean
    public TopicExchange fnbExchange() {
        return new TopicExchange(FNB_EXCHANGE);
    }

    @Bean
    public Queue fnbQueue() {
        return new Queue(FNB_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // Bind payment queue to receive FnB events
    @Bean
    public Binding fnbToPaymentBinding() {
        return BindingBuilder
                .bind(new Queue(PAYMENT_QUEUE, true))
                .to(fnbExchange())
                .with(FNB_ORDER_CREATED_KEY);
    }

    // Bind fnb queue to receive payment success events
    @Bean
    public Binding paymentSuccessToFnbBinding() {
        return BindingBuilder
                .bind(fnbQueue())
                .to(fnbExchange())
                .with(PAYMENT_FNB_SUCCESS_KEY);
    }
}
