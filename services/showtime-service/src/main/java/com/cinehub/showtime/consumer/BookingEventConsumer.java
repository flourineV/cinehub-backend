package com.cinehub.showtime.consumer;

import com.cinehub.showtime.config.RabbitConfig;
import com.cinehub.showtime.events.BookingStatusUpdatedEvent;
import com.cinehub.showtime.events.BookingSeatMappedEvent;
import com.cinehub.showtime.events.SeatUnlockedEvent;
import com.cinehub.showtime.service.SeatLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final SeatLockService seatLockService;

    @RabbitListener(queues = RabbitConfig.SHOWTIME_QUEUE)
    public void handleBookingEvents(
            @Payload Map<String, Object> raw,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        try {
            Object dataObj = raw.get("data");
            switch (routingKey) {

                case RabbitConfig.BOOKING_SEAT_MAPPED_KEY -> {
                    BookingSeatMappedEvent event = objectMapper.convertValue(dataObj,
                            BookingSeatMappedEvent.class);
                    log.info("✨ Received mapped event. Saving bookingId {} to seat locks.", event.bookingId());
                    // ✅ HÀM MỚI: Cập nhật giá trị lock key trong Redis để bao gồm bookingId
                    seatLockService.mapBookingIdToSeatLocks(event);
                }

                case RabbitConfig.BOOKING_CONFIRMED_KEY -> {
                    BookingStatusUpdatedEvent event = objectMapper.convertValue(dataObj,
                            BookingStatusUpdatedEvent.class);
                    // Ghế đã được thanh toán, chuyển sang trạng thái BOOKED
                    seatLockService.confirmBookingSeats(event);
                }

                // 2. CANCELLED, 3. EXPIRED, 4. RELEASE REQUEST (Tất cả đều giải phóng ghế)
                case RabbitConfig.BOOKING_CANCELLED_KEY,
                        RabbitConfig.BOOKING_EXPIRED_KEY -> {
                    BookingStatusUpdatedEvent event = objectMapper.convertValue(dataObj,
                            BookingStatusUpdatedEvent.class);
                    // Booking hủy/hết hạn, chuyển ghế từ LOCKED -> AVAILABLE
                    seatLockService.releaseSeatsByBookingStatus(event);
                }

                // Lệnh mở khóa khẩn cấp từ Booking (Thường là Payment Failed)
                case RabbitConfig.SEAT_RELEASE_REQUEST_KEY -> {
                    SeatUnlockedEvent event = objectMapper.convertValue(dataObj,
                            SeatUnlockedEvent.class);
                    // Thực hiện lệnh mở khóa ngay lập tức
                    seatLockService.releaseSeatsByCommand(event);
                }

                default -> log.warn("⚠️ Received event with unknown Routing Key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process event with RK {}: {}", routingKey, e.getMessage(), e);
        }
    }
}