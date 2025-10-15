package com.cinehub.booking.consumer;

import com.cinehub.booking.events.showtime.SeatLockedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowtimeEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingService bookingService;

    @RabbitListener(queues = "booking.seat.events.queue")
    public void consume(@Payload Map<String, Object> rawMessage) {
        try {
            String type = (String) rawMessage.get("type");
            Object dataObj = rawMessage.get("data");

            if (type == null)
                return;

            switch (type) {
                case "SeatLocked" -> {
                    SeatLockedEvent data = objectMapper.convertValue(dataObj, SeatLockedEvent.class);
                    log.info("🔓 Seatlocked received: {}", data);
                    bookingService.handleSeatLocked(data);
                }
                case "SeatUnlocked" -> {
                    SeatUnlockedEvent data = objectMapper.convertValue(dataObj, SeatUnlockedEvent.class);
                    log.info("🔓 SeatUnlocked received: {}", data);
                    // Optional: xử lý hủy booking nếu cần
                }
                default -> {
                    log.warn("⚠️ Unknown event type: {}. Full payload: {}", type, rawMessage);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error parsing Showtime event: {}", e.getMessage(), e);
        }
    }
}
