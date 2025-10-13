package com.cinehub.booking.controller;

import com.cinehub.booking.dto.request.BookingStatusRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.BookingStatusResponse;
import com.cinehub.booking.service.BookingService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping("/user/{userId}")
    public List<BookingResponse> getBookingsByUser(@PathVariable UUID userId) {
        return bookingService.getBookingsByUser(userId);
    }

    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return bookingService.updateBookingStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
    }

    // Endpoint cho API Polling
    @GetMapping("/status")
    public ResponseEntity<BookingStatusResponse> getBookingStatus(
            @RequestBody BookingStatusRequest request) { // Hoặc dùng @RequestParam

        BookingStatusResponse response = bookingService.checkBookingStatus(
                request.getUserId(),
                request.getShowtimeId());

        return ResponseEntity.ok(response);
    }
}
