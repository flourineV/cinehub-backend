package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.response.ShowtimeStatsResponse;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.ShowtimeStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/stats")
@RequiredArgsConstructor
public class ShowtimeStatsController {

    private final ShowtimeStatsService showtimeStatsService;

    @GetMapping("/overview")
    public ResponseEntity<ShowtimeStatsResponse> getOverview(
            @RequestParam(required = false) UUID theaterId) {
        AuthChecker.requireManagerOrAdmin();

        // Manager must provide theaterId
        if (AuthChecker.isManager() && theaterId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manager must provide theaterId parameter");
        }

        return ResponseEntity.ok(showtimeStatsService.getOverview(theaterId));
    }
}
