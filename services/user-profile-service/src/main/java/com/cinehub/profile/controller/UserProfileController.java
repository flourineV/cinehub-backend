package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.UpdateLoyaltyRequest;
import com.cinehub.profile.dto.request.UserProfileRequest;
import com.cinehub.profile.dto.request.UserProfileUpdateRequest;
import com.cinehub.profile.dto.response.RankAndDiscountResponse;
import com.cinehub.profile.dto.response.UserProfileResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.security.InternalAuthChecker;
import com.cinehub.profile.service.UserProfileService;
import com.cinehub.profile.service.PromotionEmailService;
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
    private final PromotionEmailService promotionEmailService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody UserProfileRequest request) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(profileService.createProfile(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {

        // Allow internal service call
        if (internalKey != null) {
            internalAuthChecker.requireInternal(internalKey);
            return profileService.getProfileByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        // Require authentication for user/admin/manager access
        AuthChecker.requireAuthenticated();

        // Admin and Manager can view any profile
        String userRole = AuthChecker.getRoleOrNull();

        if ("ADMIN".equalsIgnoreCase(userRole) || "MANAGER".equalsIgnoreCase(userRole)) {
            return profileService.getProfileByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        // Regular users can only view their own profile
        UUID currentUserId = UUID.fromString(AuthChecker.getUserIdOrThrow());

        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }

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
            @RequestBody UpdateLoyaltyRequest request,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        internalAuthChecker.requireInternal(internalKey);
        return ResponseEntity.ok(profileService.updateLoyaltyAndRank(userId, request));
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

    @PostMapping("/batch/names")
    public ResponseEntity<java.util.Map<UUID, String>> getBatchUserNames(
            @RequestBody List<UUID> userIds,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        internalAuthChecker.requireInternal(internalKey);
        java.util.Map<UUID, String> names = profileService.getBatchUserNames(userIds);
        return ResponseEntity.ok(names);
    }

    @GetMapping("/batch/search-userids")
    public ResponseEntity<List<UUID>> searchUserIdsByUsername(
            @RequestParam String username,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        internalAuthChecker.requireInternal(internalKey);
        List<UUID> userIds = profileService.searchUserIdsByUsername(username);
        return ResponseEntity.ok(userIds);
    }

    @PatchMapping("/{userId}/settings/promo-email")
    public ResponseEntity<UserProfileResponse> updatePromoEmailPreference(
            @PathVariable UUID userId,
            @RequestParam Boolean enable) {

        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(profileService.updateUserPromoEmailPreference(userId, enable));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam String status) {

        AuthChecker.requireAdmin();
        return ResponseEntity.ok(profileService.updateUserStatus(userId, status));
    }

    @GetMapping("/subscribed-emails")
    public ResponseEntity<List<String>> getSubscribedUsersEmails(
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalKey) {
        internalAuthChecker.requireInternal(internalKey);
        List<String> emails = promotionEmailService.getSubscribedUsersEmails();
        return ResponseEntity.ok(emails);
    }
}
