package com.cinehub.review.controller;

import com.cinehub.review.dto.AverageRatingResponse;
import com.cinehub.review.dto.RatingRequest;
import com.cinehub.review.dto.RatingResponse;
import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import com.cinehub.review.security.AuthChecker;
import com.cinehub.review.security.UserContext;
import com.cinehub.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewRequest request) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        AuthChecker.requireAuthenticated();
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByMovie(@PathVariable UUID movieId) {
        // Public endpoint - no auth required
        return ResponseEntity.ok(reviewService.getReviewsByMovie(movieId));
    }

    @GetMapping("/movie/{movieId}/average-rating")
    public ResponseEntity<AverageRatingResponse> getAverageRating(@PathVariable UUID movieId) {
        // Public endpoint - no auth required
        return ResponseEntity.ok(reviewService.getAverageRating(movieId));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ReviewResponse> reportReview(@PathVariable UUID id) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(reviewService.reportReview(id));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable UUID id) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(reviewService.hideReview(id));
    }

    @PostMapping("/movie/{movieId}/rate")
    public ResponseEntity<RatingResponse> upsertRating(
            @PathVariable UUID movieId,
            @RequestBody RatingRequest request) {
        AuthChecker.requireAuthenticated();

        // Get userId from authentication context
        String userIdStr = AuthChecker.getUserIdOrThrow();
        UUID userId = UUID.fromString(userIdStr);
        request.setUserId(userId);

        return ResponseEntity.ok(reviewService.upsertRating(movieId, request));
    }

    @GetMapping("/movie/{movieId}/my-rating")
    public ResponseEntity<RatingResponse> getMyRating(@PathVariable UUID movieId) {
        AuthChecker.requireAuthenticated();

        String userIdStr = AuthChecker.getUserIdOrThrow();
        UUID userId = UUID.fromString(userIdStr);
        RatingResponse rating = reviewService.getMyRating(movieId, userId);

        if (rating == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(rating);
    }
}
