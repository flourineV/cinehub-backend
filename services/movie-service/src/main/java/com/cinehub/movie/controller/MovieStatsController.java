package com.cinehub.movie.controller;

import com.cinehub.movie.dto.response.MovieStatsResponse;
import com.cinehub.movie.dto.response.MovieMonthlyStatsResponse;
import com.cinehub.movie.security.AuthChecker;
import com.cinehub.movie.service.MovieStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies/stats")
@RequiredArgsConstructor
public class MovieStatsController {

    private final MovieStatsService movieStatsService;

    @GetMapping("/overview")
    public ResponseEntity<MovieStatsResponse> getOverview() {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(movieStatsService.getOverview());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MovieMonthlyStatsResponse>> getMonthlyStats() {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(movieStatsService.getMonthlyStats());
    }
}
