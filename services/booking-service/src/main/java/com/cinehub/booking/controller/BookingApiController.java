package com.cinehub.booking.controller;

import com.cinehub.booking.dto.BookingRequest;
import com.cinehub.booking.dto.BookingSeatResponse;
import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingApiController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingSeatResponse> createBooking(@RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(request.getUserId(), request.getShowtimeId(),
                request.getSeatId());
        BookingSeatResponse response = BookingSeatResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .seatId(booking.getSeatId())
                .bookingTime(booking.getBookingTime())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingSeatResponse>> getAllBookings() {
        List<BookingSeatResponse> bookings = bookingService.getAllBookings().stream()
                .map(b -> BookingSeatResponse.builder()
                        .id(b.getId())
                        .userId(b.getUserId())
                        .showtimeId(b.getShowtimeId())
                        .seatId(b.getSeatId())
                        .bookingTime(b.getBookingTime())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }
}
