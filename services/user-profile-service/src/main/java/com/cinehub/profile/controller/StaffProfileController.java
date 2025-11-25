package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.StaffProfileRequest; // Import DTO
import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.StaffProfileService;
import jakarta.validation.Valid; // Import Valid
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffProfileController {

    private final StaffProfileService staffService;

    @PostMapping
    public ResponseEntity<StaffProfile> createStaff(@RequestBody @Valid StaffProfileRequest request) {

        AuthChecker.requireManagerOrAdmin();

        // Lấy dữ liệu từ Request Body (DTO) truyền vào Service
        StaffProfile created = staffService.createStaff(
                request.getUserProfileId(),
                request.getCinemaId(),
                request.getStartDate());

        return ResponseEntity.ok(created);
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<StaffProfile>> getStaffByCinema(@PathVariable UUID cinemaId) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(staffService.getStaffByCinema(cinemaId));
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<StaffProfile> getStaffByUserProfile(@PathVariable UUID userProfileId) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(staffService.getStaffByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<StaffProfile>> getAllStaff() {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID staffId) {
        AuthChecker.requireManagerOrAdmin();
        staffService.deleteStaff(staffId);
        return ResponseEntity.noContent().build();
    }
}