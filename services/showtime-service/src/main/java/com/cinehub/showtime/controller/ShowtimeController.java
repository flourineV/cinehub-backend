package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.ShowtimeRequest;
import com.cinehub.showtime.dto.response.ShowtimeResponse;
import com.cinehub.showtime.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/showtimes") 
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(@RequestBody ShowtimeRequest request) {
        ShowtimeResponse response = showtimeService.createShowtime(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@PathVariable String id) {
        ShowtimeResponse response = showtimeService.getShowtimeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getAllShowtimes() {
        List<ShowtimeResponse> responseList = showtimeService.getAllShowtimes();
        return ResponseEntity.ok(responseList);
    }
    
    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByMovie(@PathVariable String movieId) {
        List<ShowtimeResponse> responseList = showtimeService.getShowtimesByMovie(movieId);
        return ResponseEntity.ok(responseList);
    }
    
    // Endpoint: GET /api/showtimes/by-theater?theaterId={id}&startDate=...&endDate=...
    @GetMapping("/by-theater")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByTheaterAndDate(
            @RequestParam String theaterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<ShowtimeResponse> responseList = showtimeService.getShowtimesByTheaterAndDate(theaterId, startDate, endDate);
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @PathVariable String id,
            @RequestBody ShowtimeRequest request) {
        
        ShowtimeResponse response = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable String id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}