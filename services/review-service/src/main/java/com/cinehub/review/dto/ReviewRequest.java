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
    private int rating;
    private String comment;
}
