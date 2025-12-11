package com.cinehub.profile.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfileRequest {

    @NotNull(message = "Staff profile ID is required")
    private UUID userProfileId;

    @NotNull(message = "CinemaName is required")
    private String cinemaName;

    @NotNull(message = "HireDate is required")
    private LocalDate hireDate;
}
