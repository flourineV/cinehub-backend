package com.cinehub.auth.controller;

import com.cinehub.auth.dto.response.StatsOverviewResponse;
import com.cinehub.auth.security.AuthChecker;
import com.cinehub.auth.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview() {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(statsService.getOverview());
    }

}
