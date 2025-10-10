// package com.cinehub.showtime.consumer;

// import com.cinehub.showtime.events.BookingStatusUpdatedEvent;
// import com.cinehub.showtime.service.SeatStatusUpdateService;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Component;

// import java.util.Map;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// // public class BookingEventConsumer {

// private final ObjectMapper objectMapper;
// private final SeatStatusUpdateService seatStatusUpdateService;

// @RabbitListener(queues = "showtime.booking.events.queue") // Queue nhận từ
// booking.exchange
// public void handleBookingStatusEvent(@Payload Map<String, Object> raw) {
// try {
// String type = (String) raw.get("type");
// Object dataObj = raw.get("data");

// if (type == null)
// return;

// switch (type) {
// case "BookingStatusUpdatedEvent" -> {
// BookingStatusUpdatedEvent event = objectMapper.convertValue(dataObj,
// BookingStatusUpdatedEvent.class);
// seatStatusUpdateService.updateSeatStatusFromBooking(event);
// }
// default -> log.warn("⚠️ Unknown booking event type: {}", type);
// }
// } catch (Exception e) {
// log.error("❌ Failed to process booking event: {}", e.getMessage(), e);
// }
// }
// }
