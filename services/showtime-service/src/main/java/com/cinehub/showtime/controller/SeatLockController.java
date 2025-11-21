package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.dto.request.SeatReleaseRequest;
import com.cinehub.showtime.dto.request.SingleSeatLockRequest;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.SeatLockService;
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

        @PostMapping("/lock-single")
        public ResponseEntity<SeatLockResponse> lockSingleSeat(@RequestBody SingleSeatLockRequest req) {
                log.info("API: Locking single seat {} for showtime {}", req.getSelectedSeat().getSeatId(),
                                req.getShowtimeId());
                SeatLockResponse response = seatLockService.lockSingleSeat(req);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/unlock-single")
        public ResponseEntity<SeatLockResponse> unlockSingleSeat(
                        @RequestParam UUID showtimeId,
                        @RequestParam UUID seatId,
                        @RequestParam(required = false) UUID userId,
                        @RequestParam(required = false) UUID guestSessionId) {
                log.info("API: Unlocking single seat {} for showtime {}", seatId, showtimeId);
                SeatLockResponse response = seatLockService.unlockSingleSeat(showtimeId, seatId, userId,
                                guestSessionId);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/lock")
        public ResponseEntity<List<SeatLockResponse>> lockSeats(
                        @RequestBody SeatLockRequest req) {

                log.info("API: Locking {} seats for showtime {}", req.getSelectedSeats().size(), req.getShowtimeId());
                return ResponseEntity.ok(seatLockService.lockSeats(req));
        }

        @PostMapping("/release")
        public ResponseEntity<List<SeatLockResponse>> releaseSeats(
                        @RequestBody SeatReleaseRequest req,
                        @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {

                AuthChecker.requireManagerOrAdmin();
                log.info("API: Releasing {} seats for booking {} (Reason: {})",
                                req.getSeatIds().size(), req.getBookingId(), req.getReason());
                return ResponseEntity.ok(
                                seatLockService.releaseSeats(req.getShowtimeId(), req.getSeatIds(), req.getBookingId(),
                                                req.getReason()));
        }

        @GetMapping("/status")
        public ResponseEntity<SeatLockResponse> seatStatus(
                        @RequestParam UUID showtimeId,
                        @RequestParam UUID seatId) {
                return ResponseEntity.ok(seatLockService.seatStatus(showtimeId, seatId));
        }
}
