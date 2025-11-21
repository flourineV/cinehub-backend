package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.dto.response.ShowtimeSeatsLayoutResponse;
import com.cinehub.showtime.dto.request.UpdateSeatStatusRequest;
import com.cinehub.showtime.dto.request.BatchInitializeSeatsRequest;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.security.InternalAuthChecker;
import com.cinehub.showtime.service.ShowtimeSeatService;
import com.cinehub.showtime.websocket.SeatLockWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeSeatController {

    private final ShowtimeSeatService showtimeSeatService;
    private final SeatLockWebSocketHandler webSocketHandler;
    private final InternalAuthChecker internalAuthChecker;

    @GetMapping("/{showtimeId}/seats")
    public ResponseEntity<ShowtimeSeatsLayoutResponse> getSeatsByShowtime(@PathVariable UUID showtimeId) {
        ShowtimeSeatsLayoutResponse response = showtimeSeatService.getSeatsByShowtime(showtimeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{showtimeId}/seats/{seatId}/status")
    public ResponseEntity<ShowtimeSeatResponse> updateSeatStatus(
            @PathVariable UUID showtimeId,
            @PathVariable UUID seatId,
            @RequestBody UpdateSeatStatusRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {

        internalAuthChecker.requireInternal(internalKey);

        request.setShowtimeId(showtimeId);
        request.setSeatId(seatId);

        ShowtimeSeatResponse response = showtimeSeatService.updateSeatStatus(request);

        // Push WebSocket update
        webSocketHandler.broadcastToShowtime(showtimeId, response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize-seats")
    public ResponseEntity<String> initializeSeats(@RequestBody BatchInitializeSeatsRequest request) {
        AuthChecker.requireManagerOrAdmin();
        int count = showtimeSeatService.batchInitializeSeats(request.getShowtimeIds());
        return ResponseEntity.ok("Seats initialized successfully for " + count + " showtimes");
    }
}
