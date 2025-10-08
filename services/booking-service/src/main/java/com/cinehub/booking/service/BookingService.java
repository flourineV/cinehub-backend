package com.cinehub.booking.service;

import com.cinehub.booking.entity.*;
import com.cinehub.booking.dto.*;
import com.cinehub.booking.repository.BookingRepository;
import com.cinehub.booking.config.SeatAlreadyLockedException;
import com.cinehub.booking.client.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class BookingService {

        private final BookingRepository bookingRepository;
        private final ShowtimeClient showtimeClient;
        private final SeatLockClient seatLockClient;
        private final PricingClient pricingClient;

        @Transactional
        public BookingResponse createBooking(BookingRequest request) {
                // Lấy thông tin suất chiếu
                ShowtimeResponse showtime = showtimeClient.getShowtimeById(request.getShowtimeId());
                if (showtime == null) {
                        throw new RuntimeException("Showtime not found: " + request.getShowtimeId());
                }

                AtomicReference<BigDecimal> totalSeatPrice = new AtomicReference<>(BigDecimal.ZERO);

                // Tạo tạm bookingId để lock seat
                UUID tempBookingId = UUID.randomUUID();

                // Danh sách seat đã booking
                List<BookingSeat> bookedSeats = request.getSeatIds().stream().map(seatId -> {
                        // Lock ghế
                        SeatLockResponse lockResponse = seatLockClient.lockSeat(
                                request.getShowtimeId(),
                                seatId,
                                tempBookingId
                        );

                        if (!"LOCKED".equals(lockResponse.getStatus())) {
                        throw new SeatAlreadyLockedException(seatId.toString());
                        }

                        // Lấy seatType từ lockResponse
                        String seatType = lockResponse.getSeatType();

                        // Lấy giá ghế theo seatType từ pricing-service
                        SeatPriceResponse seatPriceResponse = pricingClient.getSeatPrice(seatType);
                        BigDecimal seatPrice = seatPriceResponse != null ? seatPriceResponse.getPrice() : BigDecimal.ZERO;

                        // Cộng dồn tổng
                        totalSeatPrice.set(totalSeatPrice.get().add(seatPrice));

                        // Tạo đối tượng BookingSeat
                        return BookingSeat.builder()
                                .seatId(seatId)
                                .price(seatPrice)
                                .status(SeatStatus.RESERVED)
                                .build();
                }).collect(Collectors.toList());
                
                // Lấy giá combo (nếu có)
                BigDecimal comboTotal = BigDecimal.ZERO;
                if (request.getComboIds() != null && !request.getComboIds().isEmpty()) {
                        comboTotal = pricingClient.getCombos(request.getComboIds()).stream()
                                .map(ComboResponse::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                BigDecimal subtotal = totalSeatPrice.get().add(comboTotal);

                // Áp dụng khuyến mãi
                BigDecimal discount = BigDecimal.ZERO;
                if (request.getPromotionIds() != null && !request.getPromotionIds().isEmpty()) {
                discount = pricingClient.getPromotions(request.getPromotionIds()).stream()
                        .map(promo -> subtotal.multiply(BigDecimal.valueOf(promo.getDiscountPercent()).divide(BigDecimal.valueOf(100))))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                BigDecimal finalTotal = subtotal.subtract(discount);

                // Tạo booking chính thức
                Booking booking = Booking.builder()
                                .id(UUID.randomUUID())
                                .userId(request.getUserId())
                                .showtimeId(request.getShowtimeId())
                                .totalPrice(finalTotal)
                                .status(BookingStatus.PENDING)
                                .build();

                // Gán lại quan hệ
                bookedSeats.forEach(seat -> seat.setBooking(booking));
                booking.setSeats(bookedSeats);

                bookingRepository.save(booking);
                return mapToResponse(booking);
        }

        public BookingResponse getBookingById(UUID id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
                return mapToResponse(booking);
        }

        public List<BookingResponse> getBookingsByUser(UUID userId) {
                return bookingRepository.findByUserId(userId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public BookingResponse updateBookingStatus(UUID id, String status) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
                booking.setStatus(BookingStatus.valueOf(status.toUpperCase()));
                bookingRepository.save(booking);

                return mapToResponse(booking);
        }

        @Transactional
        public void deleteBooking(UUID id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Booking not found: " + id));
                bookingRepository.delete(booking);
        }

        // helper function
        private BookingResponse mapToResponse(Booking booking) {
                List<BookingSeatResponse> seatResponses = booking.getSeats().stream()
                                .map(seat -> BookingSeatResponse.builder()
                                                .seatId(seat.getSeatId())
                                                .price(seat.getPrice())
                                                .status(seat.getStatus().name())
                                                .build())
                                .collect(Collectors.toList());

                return BookingResponse.builder()
                                .bookingId(booking.getId())
                                .userId(booking.getUserId())
                                .showtimeId(booking.getShowtimeId())
                                .totalPrice(booking.getTotalPrice())
                                .status(booking.getStatus().name())
                                .seats(seatResponses)
                                .createdAt(booking.getCreatedAt())
                                .updatedAt(booking.getUpdatedAt())
                                .build();
        }
}
