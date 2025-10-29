package com.cinehub.review.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cinehub.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByMovieIdAndStatus(UUID movieId, com.cinehub.review.entity.ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movieId = :movieId AND r.status = 'VISIBLE'")
    Double findAverageRatingByMovieId(UUID movieId);
}
