package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.SeatRequest;
import com.cinehub.showtime.dto.response.SeatResponse;
import com.cinehub.showtime.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats") 
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@RequestBody SeatRequest request) {
        SeatResponse response = seatService.createSeat(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable String id) {
        SeatResponse response = seatService.getSeatById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        List<SeatResponse> responseList = seatService.getAllSeats();
        return ResponseEntity.ok(responseList);
    }
    
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByRoomId(@PathVariable String roomId) {
        List<SeatResponse> responseList = seatService.getSeatsByRoomId(roomId);
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable String id,
            @RequestBody SeatRequest request) {
        
        SeatResponse response = seatService.updateSeat(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable String id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}