package com.cinehub.profile.dto.request;

import com.cinehub.profile.entity.UserProfile.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 3, max = 30, message = "Username must be between 3–30 characters")
    private String username;

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    private Gender gender;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 20)
    private String nationalId;

    @Size(max = 255)
    private String address;
}
