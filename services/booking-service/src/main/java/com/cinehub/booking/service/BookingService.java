package com.cinehub.booking.service;

import com.cinehub.booking.client.PricingClient;
import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.BookingStatusResponse;
import com.cinehub.booking.dto.response.PollingStatus;
import com.cinehub.booking.entity.*;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.showtime.SeatLockedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;
import com.cinehub.booking.events.payment.PaymentCompletedEvent;
import com.cinehub.booking.events.payment.PaymentFailedEvent;
import com.cinehub.booking.repository.BookingRepository;
import com.cinehub.booking.producer.BookingProducer;
import com.cinehub.booking.dto.external.SeatSelectionDetail;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

        private final BookingRepository bookingRepository;
        private final PricingClient pricingClient;
        private final BookingProducer bookingProducer;

        @Transactional
        public void handleSeatLocked(SeatLockedEvent data) {
                log.info("🎟️ Received SeatLocked event: showtime={}, seats={}, user={}",
                                data.showtimeId(), data.seatIds(), data.userId());

                // 1️⃣ Tạo Booking mới
                Booking booking = Booking.builder()
                                .id(UUID.randomUUID())
                                .userId(data.userId())
                                .showtimeId(data.showtimeId())
                                .status(BookingStatus.PENDING)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                // 2️⃣ Tính giá từng ghế theo seatType tương ứng
                List<BookingSeat> seats = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;

                for (int i = 0; i < data.seatIds().size(); i++) {
                        UUID seatId = data.seatIds().get(i);
                        String seatType = data.seatTypes().size() > i ? data.seatTypes().get(i) : "STANDARD";

                        SeatPriceResponse seatPrice = pricingClient.getSeatPrice(seatType);
                        BigDecimal price = seatPrice != null ? seatPrice.getBasePrice() : BigDecimal.ZERO;

                        seats.add(BookingSeat.builder()
                                        .seatId(seatId)
                                        .price(price)
                                        .status(SeatStatus.RESERVED)
                                        .booking(booking)
                                        .build());

                        total = total.add(price);
                }

                booking.setSeats(seats);
                booking.setTotalPrice(total);

                // 3️⃣ Lưu DB
                bookingRepository.save(booking);

                log.info("✅ Booking created: {} | total={} | seats={}",
                                booking.getId(), total, seats.size());

                bookingProducer.sendBookingCreatedEvent(
                                new BookingCreatedEvent(
                                                booking.getId(),
                                                booking.getUserId(),
                                                booking.getShowtimeId(),
                                                booking.getSeats().stream()
                                                                .map(BookingSeat::getSeatId)
                                                                .toList(),
                                                booking.getTotalPrice()));
        }

        // … phần imports & class như bạn đang có …

        @Transactional
        public void onPaymentSuccess(PaymentCompletedEvent evt) {
                var booking = bookingRepository.findById(evt.bookingId())
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + evt.bookingId()));

                if (booking.getStatus() == BookingStatus.CONFIRMED)
                        return; // idempotent
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                bookingProducer.sendBookingStatusUpdatedEvent(
                                new BookingStatusUpdatedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                booking.getSeats().stream().map(BookingSeat::getSeatId).toList(),
                                                "CONFIRMED"));
                log.info("✅ Booking {} confirmed by payment", booking.getId());
        }

        @Transactional
        public void onPaymentFailed(PaymentFailedEvent evt) {
                var booking = bookingRepository.findById(evt.bookingId())
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + evt.bookingId()));

                if (booking.getStatus() == BookingStatus.CANCELLED)
                        return; // idempotent
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                bookingProducer.sendBookingStatusUpdatedEvent(
                                new BookingStatusUpdatedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                booking.getSeats().stream().map(BookingSeat::getSeatId).toList(),
                                                "CANCELLED"));
                log.info("🚫 Booking {} cancelled (payment failed)", booking.getId());
        }

        // ========== REST CRUD cho FE hoặc admin test ==========

        public BookingResponse getBookingById(UUID id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
                return mapToResponse(booking);
        }

        public List<BookingResponse> getBookingsByUser(UUID userId) {
                return bookingRepository.findByUserId(userId).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional
        public void deleteBooking(UUID id) {
                bookingRepository.deleteById(id);
        }

        @Transactional
        public BookingResponse updateBookingStatus(UUID id, String status) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));

                booking.setStatus(BookingStatus.valueOf(status.toUpperCase()));
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                log.info("🔄 Booking {} status updated to {}", booking.getId(), booking.getStatus());
                return mapToResponse(booking);
        }

        // Helper mapper
        private BookingResponse mapToResponse(Booking booking) {
                return BookingResponse.builder()
                                .bookingId(booking.getId())
                                .userId(booking.getUserId())
                                .showtimeId(booking.getShowtimeId())
                                .status(booking.getStatus().name())
                                .totalPrice(booking.getTotalPrice())
                                .createdAt(booking.getCreatedAt())
                                .updatedAt(booking.getUpdatedAt())
                                .build();
        }
}
