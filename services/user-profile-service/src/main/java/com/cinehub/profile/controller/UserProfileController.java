package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.UserProfileRequest;
import com.cinehub.profile.dto.request.UserProfileUpdateRequest;
import com.cinehub.profile.dto.response.RankAndDiscountResponse;
import com.cinehub.profile.dto.response.UserProfileResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.security.InternalAuthChecker;
import com.cinehub.profile.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/profiles/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;
    private final InternalAuthChecker internalAuthChecker;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody UserProfileRequest request) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(profileService.createProfile(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable UUID userId) {
        AuthChecker.requireAuthenticated();
        return profileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> replaceProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PatchMapping("/{userId}/loyalty")
    public ResponseEntity<UserProfileResponse> updateLoyalty(
            @PathVariable UUID userId,
            @RequestBody Integer loyaltyPoint,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        internalAuthChecker.requireInternal(internalKey);
        return ResponseEntity.ok(profileService.updateLoyaltyAndRank(userId, loyaltyPoint));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteProfile(@PathVariable UUID userId) {
        AuthChecker.requireAdmin();
        profileService.deleteProfile(userId);
        return ResponseEntity.ok("Profile deleted successfully");
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchProfiles(
            @RequestParam(required = false) String keyword) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(profileService.searchProfiles(keyword));
    }

    @GetMapping("/{userId}/rank")
    public ResponseEntity<RankAndDiscountResponse> getUserRankAndDiscount(@PathVariable UUID userId) {
        RankAndDiscountResponse response = profileService.getRankAndDiscount(userId);
        return ResponseEntity.ok(response);
    }
}
