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
@RequestMapping("/api/profiles")
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
            @Valid @RequestBody UserProfileRequest request) {
        // Dùng DTO đầy đủ và gọi hàm updateProfile cũ (cho PUT)
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID userId,
            // Dùng DTO cập nhật từng phần (UserProfileUpdateRequest)
            @RequestBody UserProfileUpdateRequest request) {

        // SỬA ĐỔI: Gọi hàm mới đã tích hợp logic Loyalty Point và Rank
        return ResponseEntity.ok(profileService.updateLoyaltyAndProfile(userId, request));
    }
}