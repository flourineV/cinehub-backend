package com.cinehub.review.controller;

import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
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
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(reviewService.getReviewsByMovie(movieId));
    }

    @GetMapping("/movie/{movieId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID movieId) {
        return ResponseEntity.ok(reviewService.getAverageRating(movieId));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ReviewResponse> reportReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.reportReview(id));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.hideReview(id));
    }
}
