package com.cinehub.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // === Queue (nơi NotificationService lắng nghe) ===
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    // === Exchange mà PaymentService gửi tới ===
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // === Routing key tương ứng ===
    public static final String PAYMENT_ROUTING_KEY = "key.notification.ready";

    // 1️⃣ Queue chính
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // 2️⃣ Exchange từ PaymentService
    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    // 3️⃣ Binding: nối Notification queue với Payment exchange
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(paymentExchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    // 4️⃣ Converter và Template
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
