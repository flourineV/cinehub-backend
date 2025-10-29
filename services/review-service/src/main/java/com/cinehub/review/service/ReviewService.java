package com.cinehub.review.service;

import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import com.cinehub.review.entity.Review;
import com.cinehub.review.entity.ReviewStatus;
import com.cinehub.review.repository.ReviewRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        Review review = Review.builder()
                .movieId(request.getMovieId())
                .userId(request.getUserId())
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        return toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(UUID id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        reviewRepository.save(review);
        return toResponse(review);
    }

    @Override
    public void deleteReview(UUID id) {
        reviewRepository.deleteById(id);
    }

    @Override
    public List<ReviewResponse> getReviewsByMovie(UUID movieId) {
        return reviewRepository.findByMovieIdAndStatus(movieId, ReviewStatus.VISIBLE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAverageRating(UUID movieId) {
        Double avg = reviewRepository.findAverageRatingByMovieId(movieId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public ReviewResponse reportReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setReported(true);
        reviewRepository.save(review);
        return toResponse(review);
    }

    @Override
    public ReviewResponse hideReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
        return toResponse(review);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovieId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .reported(review.isReported())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}