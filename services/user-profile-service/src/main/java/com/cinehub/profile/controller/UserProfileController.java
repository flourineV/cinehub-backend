package com.cinehub.profile.controller;

import com.cinehub.profile.dto.CreateProfileRequest;
import com.cinehub.profile.dto.UpdateProfileRequest;
import com.cinehub.profile.dto.UserProfileResponse;
import com.cinehub.profile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserProfileController {
    
    @Autowired
    private UserProfileService userProfileService;
    
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        try {
            UserProfileResponse response = userProfileService.createProfile(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable UUID userId) {
        return userProfileService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search/{identifier}")
    public ResponseEntity<UserProfileResponse> getProfileByIdentifier(@PathVariable String identifier) {
        return userProfileService.getProfileByIdentifier(identifier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllProfiles(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        List<UserProfileResponse> profiles = activeOnly 
                ? userProfileService.getActiveProfiles()
                : userProfileService.getAllProfiles();
                
        return ResponseEntity.ok(profiles);
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        return userProfileService.updateProfile(userId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        boolean deleted = userProfileService.deleteProfile(userId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchProfiles(
            @RequestParam String name) {
        
        List<UserProfileResponse> profiles = userProfileService.searchProfilesByName(name);
        return ResponseEntity.ok(profiles);
    }
    
    @PostMapping("/{userId}/loyalty-points")
    public ResponseEntity<Void> addLoyaltyPoints(
            @PathVariable UUID userId,
            @RequestParam Integer points) {
        
        userProfileService.addLoyaltyPoints(userId, points);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{userId}/loyalty-points")
    public ResponseEntity<Void> updateLoyaltyPoints(
            @PathVariable UUID userId,
            @RequestParam Integer points) {
        
        userProfileService.updateLoyaltyPoints(userId, points);
        return ResponseEntity.ok().build();
    }
}