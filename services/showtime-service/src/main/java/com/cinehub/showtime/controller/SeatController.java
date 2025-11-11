package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.SeatRequest;
import com.cinehub.showtime.dto.response.SeatResponse;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<List<SeatResponse>> createSeats(@RequestBody List<SeatRequest> requests) {
        AuthChecker.requireManagerOrAdmin();
        List<SeatResponse> responses = seatService.createSeats(requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable UUID id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByRoomId(@PathVariable UUID roomId) {
        return ResponseEntity.ok(seatService.getSeatsByRoomId(roomId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(@PathVariable UUID id, @RequestBody SeatRequest request) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable UUID id) {
        AuthChecker.requireManagerOrAdmin();
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
