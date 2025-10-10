package com.cinehub.booking.controller;

import com.cinehub.booking.dto.BookingResponse;
import com.cinehub.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
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
}
