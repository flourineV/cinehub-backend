package com.cinehub.profile.service;

import com.cinehub.profile.dto.request.UserProfileRequest;
import com.cinehub.profile.dto.request.UserProfileUpdateRequest;
import com.cinehub.profile.dto.response.UserProfileResponse;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.entity.UserRank;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRankService rankService;

    public UserProfileResponse createProfile(UserProfileRequest request) {
        if (profileRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Profile already exists for this user: " + request.getUserId());
        }

        UserRank defaultRank = rankService.findDefaultRank()
                .orElseThrow(
                        () -> new IllegalStateException("Hệ thống lỗi: Không tìm thấy Rank mặc định (min_points=0)."));

        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .email(request.getEmail())
                .username(request.getUsername())
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .address(request.getAddress())
                .rank(defaultRank)
                .build();

        return mapToResponse(profileRepository.save(profile));
    }

    public Optional<UserProfileResponse> getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::mapToResponse);
    }

    public UserProfileResponse updateProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile existing = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        if (request.getFullName() != null)
            existing.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null)
            existing.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)
            existing.setAddress(request.getAddress());
        if (request.getAvatarUrl() != null)
            existing.setAvatarUrl(request.getAvatarUrl());
        if (request.getGender() != null)
            existing.setGender(request.getGender());
        // KHÔNG CẬP NHẬT email, username, nationalId, dateOfBirth (thường là bất biến)

        return mapToResponse(profileRepository.save(existing));
    }

    public UserProfileResponse updateLoyaltyAndRank(UUID userId, Integer addedPoints) {
        UserProfile existing = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        if (addedPoints == null || addedPoints <= 0) {
            return mapToResponse(existing);
        }

        Integer currentPoints = existing.getLoyaltyPoint() != null ? existing.getLoyaltyPoint() : 0;
        Integer newLoyaltyPoint = currentPoints + addedPoints;
        existing.setLoyaltyPoint(newLoyaltyPoint);

        // 3. Tìm và Cập nhật Rank (dựa trên newLoyaltyPoint)
        rankService.findRankByLoyaltyPoint(newLoyaltyPoint)
                .ifPresent(newRank -> {
                    // Chỉ cập nhật Rank nếu Rank mới khác Rank hiện tại
                    if (existing.getRank() == null || !newRank.getId().equals(existing.getRank().getId())) {
                        existing.setRank(newRank);
                    }
                });

        return mapToResponse(profileRepository.save(existing));
    }

    // --- Phương thức Mapping (Giữ nguyên) ---
    private UserProfileResponse mapToResponse(UserProfile entity) {
        if (entity == null)
            return null;

        UserRank rank = entity.getRank();

        return UserProfileResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .gender(entity.getGender())
                .dateOfBirth(entity.getDateOfBirth())
                .phoneNumber(entity.getPhoneNumber())
                .nationalId(entity.getNationalId())
                .address(entity.getAddress())
                .loyaltyPoint(entity.getLoyaltyPoint())
                .rankName(rank != null ? rank.getName() : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}