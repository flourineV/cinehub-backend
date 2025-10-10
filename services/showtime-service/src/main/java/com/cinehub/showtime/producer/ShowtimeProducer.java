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

        public <T> void sendSeatLockedEvent(T data) {
                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "SeatLocked",
                                "v1",
                                Instant.now(),
                                data);
                rabbitTemplate.convertAndSend(
                                RabbitConfig.SHOWTIME_EXCHANGE,
                                RabbitConfig.SEAT_LOCK_ROUTING_KEY,
                                msg);
        }

        public <T> void sendSeatUnlockedEvent(T data) {
                // Cần có một Routing Key mới cho sự kiện này trong RabbitConfig
                // Giả sử bạn đã thêm: public static final String SEAT_UNLOCKED_ROUTING_KEY =
                // "key.seat.unlocked";

                var msg = new EventMessage<>(
                                UUID.randomUUID().toString(),
                                "SeatUnlocked", // Loại sự kiện khác
                                "v1",
                                Instant.now(),
                                data);

                rabbitTemplate.convertAndSend(
                                RabbitConfig.SHOWTIME_EXCHANGE,
                                RabbitConfig.SEAT_UNLOCK_ROUTING_KEY, // Sử dụng Routing Key riêng cho Unlocked
                                msg);
        }
}
