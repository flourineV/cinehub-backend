package com.cinehub.review.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AverageRatingResponse {
    private Double averageRating;
    private Long ratingCount;
}
