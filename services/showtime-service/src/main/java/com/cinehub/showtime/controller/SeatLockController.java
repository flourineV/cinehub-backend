package com.cinehub.showtime.controller;

import com.cinehub.showtime.service.SeatLockService;
import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.dto.request.SeatReleaseRequest; // ✅ Thêm DTO mới cho Release
import com.cinehub.showtime.dto.response.SeatLockResponse;
import com.cinehub.showtime.exception.IllegalSeatLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/showtimes/seat-lock")
@RequiredArgsConstructor
public class SeatLockController {

        private final SeatLockService seatLockService;

        @PostMapping("/lock")
        public ResponseEntity<List<SeatLockResponse>> lockSeats(@RequestBody SeatLockRequest req) {
                log.info("API: Received request to lock {} seats for showtime {}",
                                req.getSelectedSeats().size(), req.getShowtimeId());

                List<SeatLockResponse> responses = seatLockService.lockSeats(req);

                return ResponseEntity.ok(responses);
        }

        @PostMapping("/release")
        public ResponseEntity<List<SeatLockResponse>> releaseSeats(@RequestBody SeatReleaseRequest req) {
                log.info("API: Received request to release {} seats for booking {} (Reason: {}).",
                                req.getSeatIds().size(), req.getBookingId(), req.getReason());

                List<SeatLockResponse> responses = seatLockService.releaseSeats(
                                req.getShowtimeId(),
                                req.getSeatIds(),
                                req.getBookingId(), // Cung cấp bookingId
                                req.getReason());

                return ResponseEntity.ok(responses);
        }

        @GetMapping("/status")
        public ResponseEntity<SeatLockResponse> seatStatus(
                        @RequestParam UUID showtimeId,
                        @RequestParam UUID seatId) {

                SeatLockResponse response = seatLockService.seatStatus(showtimeId, seatId);
                return ResponseEntity.ok(response);
        }
}