package com.cinehub.booking.controller;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.PagedResponse;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.service.BookingStatisticService;
import com.cinehub.booking.security.AuthChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings/statistics")
@RequiredArgsConstructor
public class BookingStatisticController {

    private final BookingStatisticService bookingStatisticService;

    @GetMapping
    public ResponseEntity<PagedResponse<BookingResponse>> getBookingStatistics(
            // --- 1. Các tham số lọc (Filter) ---
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID showtimeId,
            @RequestParam(required = false) UUID theaterId,
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
        criteria.setUserId(userId);
        criteria.setShowtimeId(showtimeId);
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
        PagedResponse<BookingResponse> response = bookingStatisticService.getBookingsByCriteria(
                criteria, page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }
}