package com.cinehub.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingRequest {
    private UUID userId;
    private String fullName;
    private String avatarUrl;

    @Min(1)
    @Max(5)
    private Integer rating;
}
