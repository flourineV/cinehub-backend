package com.cinehub.booking.controller;

import com.cinehub.booking.dto.response.BookingStatsResponse;
import com.cinehub.booking.dto.response.RevenueStatsResponse;
import com.cinehub.booking.security.AuthChecker;
import com.cinehub.booking.service.BookingStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings/stats")
@RequiredArgsConstructor
public class BookingStatsController {

    private final BookingStatsService bookingStatsService;

    @GetMapping("/overview")
    public ResponseEntity<BookingStatsResponse> getOverview(
            @RequestParam(required = false) UUID theaterId) {
        AuthChecker.requireManagerOrAdmin();

        // Manager must provide theaterId
        if (AuthChecker.isManager() && theaterId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manager must provide theaterId parameter");
        }

        return ResponseEntity.ok(bookingStatsService.getOverview(theaterId));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueStatsResponse>> getRevenueStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID theaterId,
            @RequestParam(required = false) UUID provinceId) {
        AuthChecker.requireManagerOrAdmin();

        // Manager must provide theaterId
        if (AuthChecker.isManager() && theaterId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manager must provide theaterId parameter");
        }

        return ResponseEntity.ok(bookingStatsService.getRevenueStats(year, month, theaterId, provinceId));
    }
}
