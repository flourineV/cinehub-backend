package com.cinehub.movie.repository;

import com.cinehub.movie.entity.MovieTranslation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieTranslationRepository extends MongoRepository<MovieTranslation, UUID> {

    Optional<MovieTranslation> findByMovieIdAndLanguage(UUID movieId, String language);

    List<MovieTranslation> findByMovieId(UUID movieId);

    void deleteByMovieId(UUID movieId);
}
