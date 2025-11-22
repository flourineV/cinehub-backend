package com.cinehub.showtime.producer;

import com.cinehub.showtime.config.RabbitConfig;
import com.cinehub.showtime.events.EventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShowtimeProducer {

        private final RabbitTemplate rabbitTemplate;

        public <T> void sendSeatUnlockedEvent(T data) {
                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "SeatUnlocked",
                                "v1",
                                Instant.now(),
                                data);

                rabbitTemplate.convertAndSend(
                                RabbitConfig.SHOWTIME_EXCHANGE,
                                RabbitConfig.SEAT_UNLOCK_ROUTING_KEY,
                                msg);
        }
}