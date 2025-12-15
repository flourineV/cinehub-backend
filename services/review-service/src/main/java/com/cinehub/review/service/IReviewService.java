package com.cinehub.review.service;

import com.cinehub.review.dto.AverageRatingResponse;
import com.cinehub.review.dto.RatingRequest;
import com.cinehub.review.dto.RatingResponse;
import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import java.util.List;
import java.util.UUID;

public interface IReviewService {
    ReviewResponse createReview(ReviewRequest request);

    ReviewResponse updateReview(UUID id, ReviewRequest request);

    void deleteReview(UUID id);

    List<ReviewResponse> getReviewsByMovie(UUID movieId);

    AverageRatingResponse getAverageRating(UUID movieId);

    ReviewResponse reportReview(UUID id);

    ReviewResponse hideReview(UUID id);

    // UPSERT pattern for rating
    RatingResponse upsertRating(UUID movieId, RatingRequest request);

    RatingResponse getMyRating(UUID movieId, UUID userId);
}
