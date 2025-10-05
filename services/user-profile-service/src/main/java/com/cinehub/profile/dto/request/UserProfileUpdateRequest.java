package com.cinehub.profile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.cinehub.profile.entity.UserProfile.Gender;;

// Trong file UserProfileUpdateRequest.java
// KHÔNG có @NotNull, @NotBlank, hay @Valid cho userId và email

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    // Các trường PATCH có thể là null
    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private Gender gender;

    // Trường đặc biệt bạn muốn cập nhật:
    private Integer loyaltyPoint;
}
