package com.cinehub.review.dto;

import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {
    private UUID movieId;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private String comment; // Only comment, no rating
}
