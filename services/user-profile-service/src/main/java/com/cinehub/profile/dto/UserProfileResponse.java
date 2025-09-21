package com.cinehub.profile.dto;

import com.cinehub.profile.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    
    private UUID id;
    private UUID userId;
    private String email;
    private String username;
    private String phoneNumber;
    private String nationalId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatarUrl;
    private List<String> favoriteGenres;
    private Integer loyaltyPoint;
    private String rank;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserProfileResponse from(UserProfile userProfile) {
        return UserProfileResponse.builder()
                .id(userProfile.getId())
                .userId(userProfile.getUserId())
                .email(userProfile.getEmail())
                .username(userProfile.getUsername())
                .phoneNumber(userProfile.getPhoneNumber())
                .nationalId(userProfile.getNationalId())
                .fullName(userProfile.getFullName())
                .dateOfBirth(userProfile.getDateOfBirth())
                .gender(userProfile.getGender())
                .avatarUrl(userProfile.getAvatarUrl())
                .favoriteGenres(userProfile.getFavoriteGenres())
                .loyaltyPoint(userProfile.getLoyaltyPoint())
                .rank(userProfile.getRank())
                .status(userProfile.getStatus())
                .createdAt(userProfile.getCreatedAt())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }
}