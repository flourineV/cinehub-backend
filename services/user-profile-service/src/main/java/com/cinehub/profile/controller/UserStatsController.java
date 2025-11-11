package com.cinehub.profile.controller;

import com.cinehub.profile.dto.response.UserStatsResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    @GetMapping("/overview")
    public ResponseEntity<UserStatsResponse> getOverview() {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(userStatsService.getOverviewStats());
    }
}
