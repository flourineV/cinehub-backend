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

    /**
     * POST /api/showtimes/seat-lock/lock
     * Khóa một hoặc nhiều ghế.
     */
    @PostMapping("/lock")
    public ResponseEntity<List<SeatLockResponse>> lockSeats(@RequestBody SeatLockRequest req) {
        log.info("API: Received request to lock {} seats for showtime {}",
                req.getSelectedSeats().size(), req.getShowtimeId());

        // Không cần khối try-catch nếu bạn sử dụng @ControllerAdvice,
        // hoặc ném trực tiếp để Spring tự xử lý.
        List<SeatLockResponse> responses = seatLockService.lockSeats(
                req.getShowtimeId(),
                req.getSelectedSeats(),
                req.getUserId());

        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/showtimes/seat-lock/release
     * Giải phóng một hoặc nhiều ghế theo yêu cầu (ví dụ: user hủy).
     * Yêu cầu này thường đến từ Booking Service hoặc UI (sau khi xác thực).
     */
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

    /**
     * GET /api/showtimes/seat-lock/status
     * Kiểm tra trạng thái và TTL của một ghế.
     */
    @GetMapping("/status")
    public ResponseEntity<SeatLockResponse> seatStatus(
            @RequestParam UUID showtimeId,
            @RequestParam UUID seatId) {

        SeatLockResponse response = seatLockService.seatStatus(showtimeId, seatId);
        return ResponseEntity.ok(response);
    }
}