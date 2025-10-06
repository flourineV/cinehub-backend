package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.UserProfileRequest;
import com.cinehub.profile.dto.request.UserProfileUpdateRequest; // DTO mới
import com.cinehub.profile.dto.response.UserProfileResponse;
import com.cinehub.profile.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profiles/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(@Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(profileService.createProfile(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable UUID userId) {
        return profileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> replaceProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        // Dùng DTO đầy đủ và gọi hàm updateProfile cũ (cho PUT)
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PatchMapping("/{userId}/loyalty")
    public ResponseEntity<UserProfileResponse> updateLoyalty(
            @PathVariable UUID userId,
            @RequestBody Integer loyaltyPoint) {

        return ResponseEntity.ok(profileService.updateLoyaltyAndRank(userId, loyaltyPoint));
    }
}