package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.ManagerProfileRequest; // Import DTO
import com.cinehub.profile.entity.ManagerProfile;
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
    public ResponseEntity<ManagerProfile> createManager(@RequestBody @Valid ManagerProfileRequest request) {

        AuthChecker.requireAdmin();

        // Lấy dữ liệu từ Request Body (DTO) truyền vào Service
        ManagerProfile created = managerService.createManager(
                request.getUserProfileId(),
                request.getManagedCinemaId(),
                request.getHireDate());

        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<ManagerProfile> getManagerByUser(@PathVariable UUID userProfileId) {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getManagerByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<ManagerProfile>> getAllManagers() {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<ManagerProfile>> getManagersByCinema(@PathVariable UUID cinemaId) {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getManagersByCinema(cinemaId));
    }

    @DeleteMapping("/{managerId}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID managerId) {
        AuthChecker.requireAdmin();
        managerService.deleteManager(managerId);
        return ResponseEntity.noContent().build();
    }
}