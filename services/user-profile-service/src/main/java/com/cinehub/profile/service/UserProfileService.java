package com.cinehub.profile.service;

import com.cinehub.profile.dto.request.UserProfileRequest;
// GIẢ ĐỊNH: Import DTO mới của bạn cho cập nhật PATCH
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

    // --- Phương thức tạo Profile (Giữ nguyên) ---
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

    // --- Phương thức GET Profile (Giữ nguyên) ---
    public Optional<UserProfileResponse> getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::mapToResponse);
    }

    // --- Phương thức UPDATE/REPLACE Profile (Dùng cho PUT/PATCH cũ) ---
    // Giữ nguyên để phục vụ Controller @PutMapping (replaceProfile)
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        // ... (Logic cũ của bạn) ...
        UserProfile existing = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        // Cập nhật các trường chỉ khi Request cung cấp giá trị (PATCH semantics)
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

    // --------------------------------------------------------------------------------
    // BỔ SUNG: Phương thức cập nhật Loyalty Point và Rank (Dùng cho Controller
    // @PatchMapping)
    // --------------------------------------------------------------------------------
    public UserProfileResponse updateLoyaltyAndProfile(UUID userId, UserProfileUpdateRequest request) {
        UserProfile existing = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for userId: " + userId));

        // 1. CẬP NHẬT LOYALTY POINT VÀ RANK
        if (request.getLoyaltyPoint() != null) {
            Integer newLoyaltyPoint = request.getLoyaltyPoint();
            existing.setLoyaltyPoint(newLoyaltyPoint);

            // Tìm Rank mới dựa trên điểm
            rankService.findRankByLoyaltyPoint(newLoyaltyPoint)
                    .ifPresent(newRank -> {
                        // Chỉ cập nhật Rank nếu Rank mới khác Rank hiện tại
                        if (!newRank.getId().equals(existing.getRank().getId())) {
                            existing.setRank(newRank);
                        }
                    });
        }

        // 2. CẬP NHẬT CÁC TRƯỜNG KHÁC (nếu UpdateRequest có)
        // Đây là ví dụ, bạn cần thêm các trường khác trong UpdateRequest vào đây
        if (request.getFullName() != null)
            existing.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null)
            existing.setAvatarUrl(request.getAvatarUrl());

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