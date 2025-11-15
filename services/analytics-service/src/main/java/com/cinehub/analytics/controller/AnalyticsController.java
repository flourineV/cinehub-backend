package com.cinehub.analytics.controller;

import com.cinehub.analytics.dto.request.TrackActivityRequest;
import com.cinehub.analytics.dto.response.DailyStatsResponse;
import com.cinehub.analytics.dto.response.UserActivityResponse;
import com.cinehub.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    public ResponseEntity<Void> trackActivity(
            @RequestBody TrackActivityRequest request,
            HttpServletRequest httpRequest) {
        analyticsService.trackActivity(request, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/activities")
    public ResponseEntity<Page<UserActivityResponse>> getUserActivities(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserActivityResponse> activities = analyticsService.getUserActivities(userId, page, size);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/stats/activities")
    public ResponseEntity<Map<String, Long>> getActivityStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Long> stats = analyticsService.getActivityStats(start, end);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/visitors")
    public ResponseEntity<Long> getUniqueVisitors(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Long visitors = analyticsService.getUniqueVisitors(start, end);
        return ResponseEntity.ok(visitors);
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<List<DailyStatsResponse>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<DailyStatsResponse> stats = analyticsService.getDailyStats(start, end);
        return ResponseEntity.ok(stats);
    }
}
