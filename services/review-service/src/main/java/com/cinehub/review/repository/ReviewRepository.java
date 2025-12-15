package com.cinehub.review.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinehub.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByMovieIdAndStatus(UUID movieId, com.cinehub.review.entity.ReviewStatus status);
}
