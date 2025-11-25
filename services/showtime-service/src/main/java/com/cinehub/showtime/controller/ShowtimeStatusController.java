package com.cinehub.showtime.controller;

import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.ShowtimeStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeStatusController {

    private final ShowtimeStatusService showtimeStatusService;

    @PostMapping("/suspend-by-movie/{movieId}")
    public ResponseEntity<Map<String, Object>> suspendShowtimesByMovie(
            @PathVariable UUID movieId,
            @RequestParam(required = false, defaultValue = "Movie archived") String reason) {
        
        AuthChecker.requireManagerOrAdmin();
        
        int count = showtimeStatusService.suspendShowtimesByMovie(movieId, reason);
        
        return ResponseEntity.ok(Map.of(
            "message", "Showtimes suspended successfully",
            "movieId", movieId,
            "count", count,
            "reason", reason
        ));
    }

    @PostMapping("/{showtimeId}/suspend")
    public ResponseEntity<Map<String, String>> suspendShowtime(
            @PathVariable UUID showtimeId,
            @RequestParam(required = false, defaultValue = "Suspended by admin") String reason) {
        
        AuthChecker.requireManagerOrAdmin();
        
        showtimeStatusService.suspendShowtime(showtimeId, reason);
        
        return ResponseEntity.ok(Map.of(
            "message", "Showtime suspended successfully",
            "showtimeId", showtimeId.toString(),
            "reason", reason
        ));
    }
}
