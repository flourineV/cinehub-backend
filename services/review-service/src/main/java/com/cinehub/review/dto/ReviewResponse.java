package com.cinehub.review.dto;

import com.cinehub.review.entity.ReviewStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID movieId;
    private UUID userId;
    private String fullName;     
    private String avatarUrl;
    private int rating;
    private String comment;
    private ReviewStatus status;
    private boolean reported;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
