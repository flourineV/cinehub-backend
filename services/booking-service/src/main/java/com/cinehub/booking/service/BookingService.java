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

@Service
@RequiredArgsConstructor
public class BookingService {

        private final BookingRepository bookingRepository;
        private final ShowtimeClient showtimeClient;
        private final SeatLockClient seatLockClient;

        @Transactional
        public BookingResponse createBooking(BookingRequest request) {
                ShowtimeResponse showtime = showtimeClient.getShowtimeById(request.getShowtimeId());
                if (showtime == null) {
                        throw new RuntimeException("Showtime not found: " + request.getShowtimeId());
                }

                BigDecimal pricePerSeat = showtime.getPrice();
                BigDecimal total = pricePerSeat.multiply(BigDecimal.valueOf(request.getSeatIds().size()));

                // Tạo booking
                Booking booking = Booking.builder()
                                .id(UUID.randomUUID())
                                .userId(request.getUserId())
                                .showtimeId(request.getShowtimeId())
                                .totalPrice(total)
                                .status(BookingStatus.PENDING)
                                .build();

                for (UUID seatId : request.getSeatIds()) {
                        SeatLockResponse lockResponse = seatLockClient.lockSeat(
                                        request.getShowtimeId(),
                                        seatId,
                                        booking.getId());

                        if (!"LOCKED".equals(lockResponse.getStatus())) {
                                throw new SeatAlreadyLockedException(seatId.toString());
                        }
                }

                // Mapping seats
                List<BookingSeat> seats = request.getSeatIds().stream()
                                .map(seatId -> BookingSeat.builder()
                                                .seatId(seatId)
                                                .price(pricePerSeat)
                                                .status(SeatStatus.RESERVED)
                                                .booking(booking)
                                                .build())
                                .collect(Collectors.toList());

                booking.setSeats(seats);
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
