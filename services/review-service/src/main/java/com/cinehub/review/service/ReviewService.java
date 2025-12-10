package com.cinehub.review.service;

import com.cinehub.review.dto.RatingRequest;
import com.cinehub.review.dto.RatingResponse;
import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import com.cinehub.review.entity.Review;
import com.cinehub.review.entity.ReviewStatus;
import com.cinehub.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final WebClient bookingWebClient;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        // Kiểm tra user đã đặt vé phim chưa
        if (!hasUserBookedMovie(request.getMovieId(), request.getUserId())) {
            throw new RuntimeException("User chưa xem phim này, không thể review!");
        }
        // Tạo review mới
        Review review = Review.builder()
                .movieId(request.getMovieId())
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .reported(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        // thêm fullname & avatar từ userProfile
        return toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(UUID id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // User phải thực sự đã đặt vé mới được sửa
        if (!hasUserBookedMovie(review.getMovieId(), review.getUserId())) {
            throw new RuntimeException("User chưa xem phim này, không thể sửa review!");
        }

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

    private boolean hasUserBookedMovie(UUID movieId, UUID userId) {
        try {
            return bookingWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bookings/check")
                            .queryParam("userId", userId)
                            .queryParam("movieId", movieId)
                            .build())
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling BookingService", e);
            throw new RuntimeException("Failed to connect to BookingService");
        }
    }

    @Override
    public List<ReviewResponse> getReviewsByMovie(UUID movieId) {
        List<Review> reviews = reviewRepository.findByMovieIdAndStatus(movieId, ReviewStatus.VISIBLE);
        return reviews.stream()
                .map(review -> {
                    return toResponse(review);
                })
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

    // UPSERT pattern - auto create or update rating
    @Override
    public RatingResponse upsertRating(UUID movieId, RatingRequest request) {
        // Kiểm tra user đã đặt vé phim chưa
        if (!hasUserBookedMovie(movieId, request.getUserId())) {
            throw new RuntimeException("User chưa xem phim này, không thể rating!");
        }

        // Check if rating already exists
        Optional<Review> existingReview = reviewRepository.findByMovieIdAndUserId(movieId, request.getUserId());

        Review review;
        if (existingReview.isPresent()) {
            // UPDATE existing rating
            review = existingReview.get();
            review.setRating(request.getRating());
            review.setUpdatedAt(LocalDateTime.now());
            log.info("Updating rating for user {} on movie {}", request.getUserId(), movieId);
        } else {
            // CREATE new rating (without comment)
            review = Review.builder()
                    .movieId(movieId)
                    .userId(request.getUserId())
                    .fullName(request.getFullName())
                    .avatarUrl(request.getAvatarUrl())
                    .rating(request.getRating())
                    .comment(null) // No comment for rating-only
                    .status(ReviewStatus.VISIBLE)
                    .reported(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            log.info("Creating new rating for user {} on movie {}", request.getUserId(), movieId);
        }

        reviewRepository.save(review);
        return toRatingResponse(review);
    }

    @Override
    public RatingResponse getMyRating(UUID movieId, UUID userId) {
        Optional<Review> review = reviewRepository.findByMovieIdAndUserId(movieId, userId);
        return review.map(this::toRatingResponse).orElse(null);
    }

    // Convert Entity -> DTO
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovieId())
                .userId(review.getUserId())
                .fullName(review.getFullName())
                .avatarUrl(review.getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .reported(review.isReported())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private RatingResponse toRatingResponse(Review review) {
        return RatingResponse.builder()
                .id(review.getId())
                .movieId(review.getMovieId())
                .userId(review.getUserId())
                .fullName(review.getFullName())
                .avatarUrl(review.getAvatarUrl())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
