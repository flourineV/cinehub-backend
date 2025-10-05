package com.cinehub.profile.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.cinehub.profile.entity.UserProfile.Gender;
import com.cinehub.profile.entity.UserProfile.UserStatus;;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String nationalId;
    private String address;
    private Integer loyaltyPoint;
    private String rankName;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
