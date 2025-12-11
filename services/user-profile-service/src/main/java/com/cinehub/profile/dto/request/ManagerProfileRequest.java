package com.cinehub.profile.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerProfileRequest {

    @NotNull(message = "Manager profile ID is required")
    private UUID userProfileId;

    @NotNull(message = "ManagedCinemaName is required")
    private String managedCinemaName;

    @NotNull(message = "Work startDate is required")
    private LocalDate hireDate;

}
