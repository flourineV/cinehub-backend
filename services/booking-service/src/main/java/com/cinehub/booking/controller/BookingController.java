package com.cinehub.booking.controller;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.dto.request.CreateBookingRequest;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.PagedResponse;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.security.InternalAuthChecker;
import com.cinehub.booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cinehub.booking.security.AuthChecker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final InternalAuthChecker internalAuthChecker;

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

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID id) {
        AuthChecker.requireAuthenticated();
        UUID userId = AuthChecker.getCurrentUserId();
        BookingResponse response = bookingService.cancelBooking(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID id) {
        AuthChecker.requireAdmin();
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/search")
    public ResponseEntity<PagedResponse<BookingResponse>> getAllBookings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UUID showtimeId,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String guestEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        AuthChecker.requireAdmin();

        BookingCriteria criteria = new BookingCriteria();
        criteria.setKeyword(keyword);
        criteria.setUserId(userId);
        criteria.setUsername(username);
        criteria.setShowtimeId(showtimeId);
        criteria.setMovieId(movieId);
        criteria.setBookingCode(bookingCode);
        criteria.setStatus(status);
        criteria.setPaymentMethod(paymentMethod);
        criteria.setGuestName(guestName);
        criteria.setGuestEmail(guestEmail);
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);

        // Gọi Service
        PagedResponse<BookingResponse> response = bookingService.getBookingsByCriteria(
                criteria, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkUserBookedMovie(
            @RequestParam UUID userId,
            @RequestParam UUID movieId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        if (!internalAuthChecker.isInternal(internalKey)) {
            AuthChecker.requireAuthenticated();
        }
        ;
        boolean hasBooked = bookingService.hasUserBookedMovie(userId, movieId);
        return ResponseEntity.ok(hasBooked);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getBookingCountByUserId(
            @RequestParam UUID userId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        if (!internalAuthChecker.isInternal(internalKey)) {
            AuthChecker.requireAuthenticated();
        }
        long count = bookingService.getBookingCountByUserId(userId);
        return ResponseEntity.ok(count);
    }
}
