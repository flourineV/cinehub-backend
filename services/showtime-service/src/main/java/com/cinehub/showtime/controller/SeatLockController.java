package com.cinehub.showtime.controller;

import com.cinehub.showtime.service.SeatLockService;
import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/seat-lock")
@RequiredArgsConstructor
public class SeatLockController {

    private final SeatLockService seatLockService;

    /**
     * Khóa nhiều ghế cùng lúc. SỬA ĐỔI để truyền List<SeatSelectionDetail>.
     */
    @PostMapping("/lock")
    public ResponseEntity<List<SeatLockResponse>> lockSeats(@RequestBody SeatLockRequest req) {

        // ❌ CÁCH GỌI CŨ: req.getSeatIds() không còn đủ thông tin
        /*
         * List<SeatLockResponse> responses =
         * seatLockService.lockSeats(req.getShowtimeId(), req.getSeatIds(),
         * req.getUserId());
         */

        // ✅ CÁCH GỌI MỚI: Truyền toàn bộ List<SeatSelectionDetail>
        List<SeatLockResponse> responses = seatLockService.lockSeats(
                req.getShowtimeId(),
                req.getSelectedSeats(), // Lấy List<SeatSelectionDetail>
                req.getUserId());

        // Logic kiểm tra lỗi đơn giản:
        if (!responses.isEmpty() && responses.get(0).getStatus().equals("CONFLICT")) {
            return new ResponseEntity<>(responses, HttpStatus.CONFLICT);
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Giải phóng nhiều ghế cùng lúc. (Có thể giữ lại cách gọi cũ nếu service không
     * cần ticketType)
     */
    @PostMapping("/release")
    public ResponseEntity<List<SeatLockResponse>> releaseSeats(@RequestBody SeatLockRequest req) {

        // 💡 LƯU Ý: Đối với release, bạn chỉ cần seatId.
        // Ta có thể trích xuất List<UUID> seatIds từ List<SeatSelectionDetail>
        List<UUID> seatIdsToRelease = req.getSelectedSeats().stream()
                .map(detail -> detail.getSeatId())
                .toList();

        return ResponseEntity.ok(
                seatLockService.releaseSeats(req.getShowtimeId(), seatIdsToRelease));
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