package com.cinehub.showtime.controller;

import com.cinehub.showtime.service.SeatLockService;
import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/seat-lock")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    @PostMapping("/lock")
    public ResponseEntity<SeatLockResponse> lockSeat(@RequestBody SeatLockRequest req) {
        return ResponseEntity.ok(seatLockService.lockSeat(req.getShowtimeId(), req.getSeatId(), req.getBookingId()));
    }

    @PostMapping("/release")
    public ResponseEntity<SeatLockResponse> releaseSeat(@RequestBody SeatLockRequest req) {
        return ResponseEntity.ok(seatLockService.releaseSeat(req.getShowtimeId(), req.getSeatId()));
    }

    @GetMapping("/status")
    public ResponseEntity<SeatLockResponse> seatStatus(
            @RequestParam UUID showtimeId,
            @RequestParam UUID seatId) {
        return ResponseEntity.ok(seatLockService.seatStatus(showtimeId, seatId));
    }
}
