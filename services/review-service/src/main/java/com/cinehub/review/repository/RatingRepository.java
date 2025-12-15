package com.cinehub.review.repository;

import com.cinehub.review.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByMovieIdAndUserId(UUID movieId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.movieId = :movieId")
    Double findAverageRatingByMovieId(UUID movieId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.movieId = :movieId")
    Long countRatingsByMovieId(UUID movieId);
}
