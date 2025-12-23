package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.ManagerProfileRequest; // Import DTO
import com.cinehub.profile.dto.response.ManagerProfileResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.ManagerProfileService;
import jakarta.validation.Valid; // Import Valid
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/manager")
@RequiredArgsConstructor
public class ManagerProfileController {

    private final ManagerProfileService managerService;

    @PostMapping
    public ResponseEntity<ManagerProfileResponse> createManager(@RequestBody @Valid ManagerProfileRequest request) {

        AuthChecker.requireAdmin();

        // Lấy dữ liệu từ Request Body (DTO) truyền vào Service
        ManagerProfileResponse created = managerService.createManager(
                request.getUserProfileId(),
                request.getManagedCinemaName(),
                request.getHireDate());

        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<ManagerProfileResponse> getManagerByUser(@PathVariable UUID userProfileId) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(managerService.getManagerByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<ManagerProfileResponse>> getAllManagers() {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @GetMapping("/cinema/{cinemaName}")
    public ResponseEntity<List<ManagerProfileResponse>> getManagersByCinema(@PathVariable String cinemaName) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(managerService.getManagersByCinema(cinemaName));
    }

    @DeleteMapping("/{managerId}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID managerId) {
        AuthChecker.requireAdmin();
        managerService.deleteManager(managerId);
        return ResponseEntity.noContent().build();
    }
}