package com.cinehub.movie.repository;

import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.entity.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieSummaryRepository extends MongoRepository<MovieSummary, UUID> {

    Page<MovieSummary> findByStatus(MovieStatus status, Pageable pageable);

    Page<MovieSummary> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<MovieSummary> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title, Pageable pageable);

    List<MovieSummary> findByStatusNotAndTitleContainingIgnoreCase(MovieStatus status, String title);

    Optional<MovieSummary> findByTmdbId(Integer tmdbId);

    List<MovieSummary> findByStatus(MovieStatus status);

    boolean existsByTmdbId(Integer tmdbId);

    void deleteByTmdbId(Integer tmdbId);

    @Query("SELECT YEAR(m.createdAt) AS year, MONTH(m.createdAt) AS month, COUNT(m.id) AS total " +
            "FROM MovieSummary m " +
            "GROUP BY YEAR(m.createdAt), MONTH(m.createdAt) " +
            "ORDER BY year ASC, month ASC")
    List<Object[]> countMoviesAddedByMonth();

    long countByStatus(MovieStatus status);

}