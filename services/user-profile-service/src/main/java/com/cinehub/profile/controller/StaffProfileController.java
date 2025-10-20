package com.cinehub.profile.controller;

import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.service.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffProfileController {

    private final StaffProfileService staffService;

    @PostMapping
    public ResponseEntity<StaffProfile> createStaff(
            @RequestParam UUID userProfileId,
            @RequestParam UUID cinemaId,
            @RequestParam LocalDate startDate) {

        StaffProfile created = staffService.createStaff(userProfileId, cinemaId, startDate);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<StaffProfile>> getStaffByCinema(@PathVariable UUID cinemaId) {
        return ResponseEntity.ok(staffService.getStaffByCinema(cinemaId));
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<StaffProfile> getStaffByUserProfile(@PathVariable UUID userProfileId) {
        return ResponseEntity.ok(staffService.getStaffByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<StaffProfile>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID staffId) {
        staffService.deleteStaff(staffId);
        return ResponseEntity.noContent().build();
    }
}
