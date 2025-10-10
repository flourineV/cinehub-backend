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

    // 🎯 Showtime chỉ cần khai báo EXCHANGE để publish
    public static final String SHOWTIME_EXCHANGE = "showtime.exchange";
    public static final String SEAT_LOCK_ROUTING_KEY = "seat.locked";
    public static final String SEAT_UNLOCK_ROUTING_KEY = "seat.unlocked";

    // 🎯 QUEUE RIÊNG của Showtime (nếu có service khác gửi event cho Showtime)
    public static final String SHOWTIME_EVENTS_QUEUE = "showtime.events.queue";

    // === EXCHANGE để publish seat events ===
    @Bean
    public DirectExchange showtimeExchange() {
        return new DirectExchange(SHOWTIME_EXCHANGE, true, false);
    }

    // === QUEUE RIÊNG của Showtime (ví dụ: để nhận booking confirmation) ===
    @Bean
    public Queue showtimeEventsQueue() {
        return new Queue(SHOWTIME_EVENTS_QUEUE, true);
    }

    // === BINDINGS cho queue của Showtime (nếu cần) ===
    // @Bean
    // public Binding bookingConfirmedBinding(...) { ... }

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
