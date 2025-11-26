package com.cinehub.movie.repository;

import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.dto.response.MovieMonthlyStatsResponse;
import com.cinehub.movie.entity.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieSummaryRepository extends MongoRepository<MovieSummary, UUID> {

    Page<MovieSummary> findByStatus(MovieStatus status, Pageable pageable);

    Page<MovieSummary> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<MovieSummary> findByStatusAndTitleContainingIgnoreCase(MovieStatus status, String title, Pageable pageable);

    List<MovieSummary> findByStatusNotAndTitleContainingIgnoreCase(MovieStatus status, String title);

    Optional<MovieSummary> findByTmdbId(Integer tmdbId);

    List<MovieSummary> findByStatus(MovieStatus status);

    List<MovieSummary> findByStatusIn(Collection<MovieStatus> statuses, Sort sort);

    boolean existsByTmdbId(Integer tmdbId);

    void deleteByTmdbId(Integer tmdbId);

    @Aggregation(pipeline = {
            "{ '$project': { 'year': { '$year': '$createdAt' }, 'month': { '$month': '$createdAt' } } }",
            "{ '$group': { '_id': { 'year': '$year', 'month': '$month' }, 'total': { '$sum': 1 } } }",
            "{ '$project': { 'year': '$_id.year', 'month': '$_id.month', 'addedMovies': '$total', '_id': 0 } }",
            "{ '$sort': { 'year': 1, 'month': 1 } }"
    })
    List<MovieMonthlyStatsResponse> countMoviesAddedByMonth();

    long countByStatus(MovieStatus status);

    List<MovieSummary> findByStatusIn(Collection<MovieStatus> statuses);

}