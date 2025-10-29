package com.cinehub.review.service;

import com.cinehub.review.dto.ReviewRequest;
import com.cinehub.review.dto.ReviewResponse;
import com.cinehub.review.dto.UserProfileResponse;
import com.cinehub.review.entity.Review;
import com.cinehub.review.entity.ReviewStatus;
import com.cinehub.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final WebClient userProfileWebClient;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        // kiểm tra user tồn tại
        UserProfileResponse userProfile = getUserProfile(request.getUserId());

        Review review = Review.builder()
                .movieId(request.getMovieId())
                .userId(request.getUserId())
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .reported(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        // thêm fullname & avatar từ userProfile
        return toResponse(review, userProfile);
    }

    @Override
    public ReviewResponse updateReview(UUID id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        UserProfileResponse userProfile = getUserProfile(request.getUserId());

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        return toResponse(review, userProfile);
    }

    @Override
    public void deleteReview(UUID id) {
        reviewRepository.deleteById(id);
    }

    @Override
    public List<ReviewResponse> getReviewsByMovie(UUID movieId) {
        List<Review> reviews = reviewRepository.findByMovieIdAndStatus(movieId, ReviewStatus.VISIBLE);
        return reviews.stream()
                .map(review -> {
                    UserProfileResponse profile = getUserProfile(review.getUserId());
                    return toResponse(review, profile);
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

        UserProfileResponse userProfile = getUserProfile(review.getUserId());
        return toResponse(review, userProfile);
    }

    @Override
    public ReviewResponse hideReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);

        UserProfileResponse userProfile = getUserProfile(review.getUserId());
        return toResponse(review, userProfile);
    }

    // Gọi UserProfileService để lấy fullname + avatarUrl
    private UserProfileResponse getUserProfile(UUID userId) {
        try {
            return userProfileWebClient.get()
                    .uri("/profiles/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserProfileResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("User not found: " + userId);
        } catch (Exception e) {
            log.error("Error calling UserProfileService", e);
            throw new RuntimeException("Failed to connect to UserProfileService");
        }
    }

    // Convert Entity -> DTO
    private ReviewResponse toResponse(Review review, UserProfileResponse profile) {
        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovieId())
                .userId(review.getUserId())
                .fullName(profile != null ? profile.getFullName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .reported(review.isReported())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
