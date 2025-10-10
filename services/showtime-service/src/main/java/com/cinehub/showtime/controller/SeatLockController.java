package com.cinehub.showtime.controller;

import com.cinehub.showtime.service.SeatLockService;
import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List; // 👈 Cần import List
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/seat-lock")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    /**
     * Khóa nhiều ghế cùng lúc. Trả về List<SeatLockResponse>.
     * Nếu xảy ra lỗi xung đột (CONFLICT), trả về HTTP 409.
     */
    @PostMapping("/lock")
    // Sửa kiểu trả về thành List<SeatLockResponse>
    public ResponseEntity<List<SeatLockResponse>> lockSeats(@RequestBody SeatLockRequest req) {
        // Giả sử service trả về List các phản hồi thành công hoặc List các phản hồi lỗi
        List<SeatLockResponse> responses = seatLockService.lockSeats(req.getShowtimeId(), req.getSeatIds(),
                req.getUserId());

        // Logic kiểm tra lỗi đơn giản: Nếu phần tử đầu tiên là CONFLICT, trả về 409
        if (!responses.isEmpty() && responses.get(0).getStatus().equals("CONFLICT")) {
            // Trả về HTTP 409 Conflict nếu có lỗi xảy ra
            return new ResponseEntity<>(responses, HttpStatus.CONFLICT);
        }

        // Trả về HTTP 200 OK nếu tất cả các ghế được khóa thành công
        return ResponseEntity.ok(responses);
    }

    /**
     * Giải phóng nhiều ghế cùng lúc. Trả về List<SeatLockResponse>.
     */
    @PostMapping("/release")
    // Sửa kiểu trả về thành List<SeatLockResponse>
    public ResponseEntity<List<SeatLockResponse>> releaseSeats(@RequestBody SeatLockRequest req) {
        // Hàm releaseSeats cần được chỉnh sửa trong service để trả về
        // List<SeatLockResponse>
        return ResponseEntity.ok(
                seatLockService.releaseSeats(req.getShowtimeId(), req.getSeatIds()));
    }

    /**
     * Kiểm tra trạng thái ghế đơn lẻ (không cần thay đổi).
     */
    @GetMapping("/status")
    public ResponseEntity<SeatLockResponse> seatStatus(
            @RequestParam UUID showtimeId,
            @RequestParam UUID seatId) {
        return ResponseEntity.ok(seatLockService.seatStatus(showtimeId, seatId));
    }
}