package com.cinehub.profile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.cinehub.profile.entity.UserProfile.Gender;;

// KHÔNG có @NotNull, @NotBlank, hay @Valid cho userId và email

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private Gender gender;

}
