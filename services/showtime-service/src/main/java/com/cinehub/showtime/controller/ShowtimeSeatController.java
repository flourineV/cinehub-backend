package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.dto.request.UpdateSeatStatusRequest;
import com.cinehub.showtime.service.ShowtimeSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeSeatController {

    private final ShowtimeSeatService showtimeSeatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<List<ShowtimeSeatResponse>> getSeatsByShowtime(@PathVariable UUID showtimeId) {
        List<ShowtimeSeatResponse> response = showtimeSeatService.getSeatsByShowtime(showtimeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{showtimeId}/seats/{seatId}/status")
    public ResponseEntity<ShowtimeSeatResponse> updateSeatStatus(
            @PathVariable UUID showtimeId,
            @PathVariable UUID seatId,
            @RequestBody UpdateSeatStatusRequest request) {

        request.setShowtimeId(showtimeId);
        request.setSeatId(seatId);

        ShowtimeSeatResponse response = showtimeSeatService.updateSeatStatus(request);

        // Broadcast đến tất cả client đang subscribe cùng showtime
        messagingTemplate.convertAndSend(
                "/topic/showtime/" + showtimeId + "/seats", response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{showtimeId}/initialize-seats")
    public ResponseEntity<String> initializeSeats(@PathVariable UUID showtimeId) {
        showtimeSeatService.initializeSeatsForShowtime(showtimeId);
        return ResponseEntity.ok("Seats initialized successfully for showtime " + showtimeId);
    }
}
