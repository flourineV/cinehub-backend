package com.cinehub.review.service;

import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import java.util.List;
import java.util.UUID;

public interface IReviewService {
    ReviewResponse createReview(ReviewRequest request);
    ReviewResponse updateReview(UUID id, ReviewRequest request);
    void deleteReview(UUID id);
    List<ReviewResponse> getReviewsByMovie(UUID movieId);
    Double getAverageRating(UUID movieId);
    ReviewResponse reportReview(UUID id);
    ReviewResponse hideReview(UUID id);
}
