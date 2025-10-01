package com.cinehub.movie.repository;

import com.cinehub.movie.entity.MovieDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface MovieDetailRepository extends MongoRepository<MovieDetail, UUID> {

    Optional<MovieDetail> findByTmdbId(Integer tmdbId);

    boolean existsByTmdbId(Integer tmdbId);

    void deleteByTmdbId(Integer tmdbId);
}