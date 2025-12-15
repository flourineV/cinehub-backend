package com.cinehub.review.service;

import com.cinehub.review.dto.AverageRatingResponse;
import com.cinehub.review.dto.RatingRequest;
import com.cinehub.review.dto.RatingResponse;
import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import com.cinehub.review.entity.Rating;
import com.cinehub.review.entity.Review;
import com.cinehub.review.entity.ReviewStatus;
import com.cinehub.review.repository.RatingRepository;
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
    private final RatingRepository ratingRepository;
    private final WebClient bookingWebClient;

    @Value("${app.internal.secret-key}")
    private String internalSecret;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        // Kiểm tra user đã đặt vé phim chưa
        if (!hasUserBookedMovie(request.getMovieId(), request.getUserId())) {
            throw new RuntimeException("User chưa xem phim này, không thể review!");
        }

        // Validate comment is not empty
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new RuntimeException("Comment không được để trống!");
        }

        // Create new review (comment only, no unique constraint)
        Review review = Review.builder()
                .movieId(request.getMovieId())
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .reported(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);
        log.info("Created comment for user {} on movie {}", request.getUserId(), request.getMovieId());

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

        // Update comment only
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new RuntimeException("Comment không được để trống!");
        }

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
                            .path("/check")
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
    public AverageRatingResponse getAverageRating(UUID movieId) {
        Double avg = ratingRepository.findAverageRatingByMovieId(movieId);
        Long count = ratingRepository.countRatingsByMovieId(movieId);
        return AverageRatingResponse.builder()
                .averageRating(avg != null ? avg : 0.0)
                .ratingCount(count != null ? count : 0L)
                .build();
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
        Optional<Rating> existingRating = ratingRepository.findByMovieIdAndUserId(movieId, request.getUserId());

        Rating rating;
        if (existingRating.isPresent()) {
            // UPDATE existing rating
            rating = existingRating.get();
            rating.setRating(request.getRating());
            rating.setUpdatedAt(LocalDateTime.now());
            log.info("Updating rating for user {} on movie {}", request.getUserId(), movieId);
        } else {
            // CREATE new rating
            rating = Rating.builder()
                    .movieId(movieId)
                    .userId(request.getUserId())
                    .fullName(request.getFullName())
                    .avatarUrl(request.getAvatarUrl())
                    .rating(request.getRating())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            log.info("Creating new rating for user {} on movie {}", request.getUserId(), movieId);
        }

        ratingRepository.save(rating);
        return toRatingResponse(rating);
    }

    @Override
    public RatingResponse getMyRating(UUID movieId, UUID userId) {
        Optional<Rating> rating = ratingRepository.findByMovieIdAndUserId(movieId, userId);
        return rating.map(this::toRatingResponse).orElse(null);
    }

    // Convert Entity -> DTO
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovieId())
                .userId(review.getUserId())
                .fullName(review.getFullName())
                .avatarUrl(review.getAvatarUrl())
                .comment(review.getComment())
                .status(review.getStatus())
                .reported(review.isReported())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private RatingResponse toRatingResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .movieId(rating.getMovieId())
                .userId(rating.getUserId())
                .fullName(rating.getFullName())
                .avatarUrl(rating.getAvatarUrl())
                .rating(rating.getRating())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}
