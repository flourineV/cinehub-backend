package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.StaffProfileRequest;
import com.cinehub.profile.dto.response.StaffProfileResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.StaffProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/staff")
@RequiredArgsConstructor
public class StaffProfileController {

    private final StaffProfileService staffService;

    @PostMapping
    public ResponseEntity<StaffProfileResponse> createStaff(@RequestBody @Valid StaffProfileRequest request) {

        AuthChecker.requireManagerOrAdmin();

        // Lấy dữ liệu từ Request Body (DTO) truyền vào Service
        StaffProfileResponse created = staffService.createStaff(
                request.getUserProfileId(),
                request.getCinemaName(),
                request.getHireDate());

        return ResponseEntity.ok(created);
    }

    @GetMapping("/cinema/{cinemaName}")
    public ResponseEntity<List<StaffProfileResponse>> getStaffByCinema(@PathVariable String cinemaName) {
        AuthChecker.requireManagerOrAdmin();
        UUID currentUserId = UUID.fromString(AuthChecker.getUserIdOrThrow());
        String userRole = AuthChecker.getRoleOrNull();
        return ResponseEntity.ok(staffService.getStaffByCinema(cinemaName, currentUserId, userRole));
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<StaffProfileResponse> getStaffByUserProfile(@PathVariable UUID userProfileId) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(staffService.getStaffByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<StaffProfileResponse>> getAllStaff() {
        AuthChecker.requireManagerOrAdmin();
        UUID currentUserId = UUID.fromString(AuthChecker.getUserIdOrThrow());
        String userRole = AuthChecker.getRoleOrNull();
        return ResponseEntity.ok(staffService.getAllStaff(currentUserId, userRole));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID staffId) {
        AuthChecker.requireManagerOrAdmin();
        staffService.deleteStaff(staffId);
        return ResponseEntity.noContent().build();
    }
}