package com.cinehub.movie.repository;

import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.entity.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface MovieSummaryRepository extends MongoRepository<MovieSummary, UUID> {

    Page<MovieSummary> findByStatus(MovieStatus status, Pageable pageable);

    Page<MovieSummary> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<MovieSummary> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title, Pageable pageable);

    Page<MovieSummary> findByStatusNotAndTitleContainingIgnoreCase(MovieStatus status, String title, Pageable pageable);

    Optional<MovieSummary> findByTmdbId(Integer tmdbId);

    boolean existsByTmdbId(Integer tmdbId);

    void deleteByTmdbId(Integer tmdbId);
}