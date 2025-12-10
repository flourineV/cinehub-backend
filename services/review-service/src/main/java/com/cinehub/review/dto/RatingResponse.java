package com.cinehub.review.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {
    private UUID id;
    private UUID movieId;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private int rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
