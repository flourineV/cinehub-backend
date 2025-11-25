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

    @NotNull(message = "CinemaId is required")
    private UUID cinemaId;

    @NotNull(message = "Work startDate is required")
    private LocalDate startDate;
}
