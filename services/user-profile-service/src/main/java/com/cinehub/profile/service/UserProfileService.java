package com.cinehub.profile.service;

import com.cinehub.profile.dto.CreateProfileRequest;
import com.cinehub.profile.dto.UpdateProfileRequest;
import com.cinehub.profile.dto.UserProfileResponse;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserProfileService {
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        log.info("Creating profile for user: {}", request.getUserId());
        
        // Check if profile already exists
        if (userProfileRepository.existsByUserId(request.getUserId())) {
            log.warn("Profile already exists for user: {}", request.getUserId());
            throw new RuntimeException("Profile already exists for this user");
        }
        
        // Create new profile
        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .email(request.getEmail())
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .loyaltyPoint(0)
                .rank("BRONZE")
                .status("ACTIVE")
                .build();
        
        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Profile created successfully for user: {}", request.getUserId());
        
        return UserProfileResponse.from(savedProfile);
    }
    
    public Optional<UserProfileResponse> getProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfileResponse::from);
    }
    
    public Optional<UserProfileResponse> getProfileByIdentifier(String identifier) {
        return userProfileRepository.findByEmailOrUsernameOrPhoneNumber(identifier)
                .map(UserProfileResponse::from);
    }
    
    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
    
    public List<UserProfileResponse> getActiveProfiles() {
        return userProfileRepository.findByStatus("ACTIVE")
                .stream()
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
    
    public Optional<UserProfileResponse> updateProfile(UUID userId, UpdateProfileRequest request) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }
        
        UserProfile profile = profileOpt.get();
        
        // Update fields if provided
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getFavoriteGenres() != null) {
            profile.setFavoriteGenres(request.getFavoriteGenres());
        }
        if (request.getRank() != null) {
            profile.setRank(request.getRank());
        }
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }
        
        UserProfile updatedProfile = userProfileRepository.save(profile);
        return Optional.of(UserProfileResponse.from(updatedProfile));
    }
    
    public boolean deleteProfile(UUID userId) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            // Soft delete by setting status to DELETED
            UserProfile profile = profileOpt.get();
            profile.setStatus("DELETED");
            userProfileRepository.save(profile);
            return true;
        }
        return false;
    }
    
    public List<UserProfileResponse> searchProfilesByName(String name) {
        return userProfileRepository.findByFullNameContaining(name)
                .stream()
                .filter(profile -> "ACTIVE".equals(profile.getStatus()))
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
    
    public void updateLoyaltyPoints(UUID userId, Integer points) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setLoyaltyPoint(points);
            // Update rank based on loyalty points
            updateRankBasedOnPoints(profile, points);
            userProfileRepository.save(profile);
        });
    }
    
    public void addLoyaltyPoints(UUID userId, Integer pointsToAdd) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            Integer currentPoints = profile.getLoyaltyPoint() != null ? profile.getLoyaltyPoint() : 0;
            Integer newPoints = currentPoints + pointsToAdd;
            profile.setLoyaltyPoint(newPoints);
            // Update rank based on loyalty points
            updateRankBasedOnPoints(profile, newPoints);
            userProfileRepository.save(profile);
        });
    }
    
    private void updateRankBasedOnPoints(UserProfile profile, Integer points) {
        if (points >= 10000) {
            profile.setRank("DIAMOND");
        } else if (points >= 5000) {
            profile.setRank("GOLD");
        } else if (points >= 1000) {
            profile.setRank("SILVER");
        } else {
            profile.setRank("BRONZE");
        }
    }
}