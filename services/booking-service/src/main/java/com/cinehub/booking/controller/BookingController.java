package com.cinehub.booking.controller;

import com.cinehub.booking.dto.request.CreateBookingRequest;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cinehub.booking.security.AuthChecker;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID id) {
        AuthChecker.requireAuthenticated();
        BookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByUser(@PathVariable UUID userId) {
        AuthChecker.requireAuthenticated();
        List<BookingResponse> bookings = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(bookings);
    }

    @PatchMapping("/{id}/finalize")
    public ResponseEntity<BookingResponse> finalizeBooking(
            @PathVariable("id") UUID bookingId,
            @Valid @RequestBody FinalizeBookingRequest request) {
        AuthChecker.requireAuthenticated();
        BookingResponse response = bookingService.finalizeBooking(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
        AuthChecker.requireAdmin();
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
