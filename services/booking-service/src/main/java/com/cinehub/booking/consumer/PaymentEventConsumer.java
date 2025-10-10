// package com.cinehub.booking.consumer;

// import com.cinehub.booking.events.PaymentCompletedEvent;
// import com.cinehub.booking.events.PaymentFailedEvent;
// import com.cinehub.booking.service.BookingService;
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
// public class PaymentEventConsumer {

// private final ObjectMapper objectMapper;
// private final BookingService bookingService;

// @RabbitListener(queues = ".queue")
// public void consume(@Payload Map<String, Object> rawMessage) {
// try {
// String type = (String) rawMessage.get("type");
// Object dataObj = rawMessage.get("data");

// if (type == null)
// return;

// switch (type) {
// case "PaymentSuccess" -> {
// PaymentCompletedEvent data = objectMapper.convertValue(dataObj,
// PaymentCompletedEvent.class);
// bookingService.updateBookingStatus(data.bookingId(), "CONFIRMED");
// }
// case "PaymentFailed" -> {
// PaymentFailedEvent data = objectMapper.convertValue(dataObj,
// PaymentFailedEvent.class);
// bookingService.updateBookingStatus(data.bookingId(), "CANCELLED");
// }
// default -> {
// // không xử lý nếu không thuộc Payment
// }
// }

// } catch (Exception e) {
// log.error("❌ Error parsing Payment event: {}", e.getMessage(), e);
// }
// }
// }
