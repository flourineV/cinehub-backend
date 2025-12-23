package com.cinehub.auth.controller;

import com.cinehub.auth.dto.response.StatsOverviewResponse;
import com.cinehub.auth.dto.response.UserRegistrationStatsResponse;
import com.cinehub.auth.security.AuthChecker;
import com.cinehub.auth.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.extensions.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@Tag(name = "Auth Statistics")
@RestController
@RequestMapping("/api/auth/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Get system statistics overview", extensions = @Extension(name = "x-order", properties = {
            @ExtensionProperty(name = "order", value = "8")
    }))
    @ApiResponse(responseCode = "200", description = "Statistics fetched successfully")
    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview() {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(statsService.getOverview());
    }

    @Operation(summary = "Get monthly user registration statistics", extensions = @Extension(name = "x-order", properties = {
            @ExtensionProperty(name = "order", value = "9")
    }))
    @ApiResponse(responseCode = "200", description = "Monthly statistics fetched successfully")
    @GetMapping("/users/monthly")
    public ResponseEntity<List<UserRegistrationStatsResponse>> getUserStatsByMonth() {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(statsService.getUserRegistrationsByMonth());
    }
}
