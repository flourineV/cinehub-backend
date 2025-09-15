package com.cinehub.movie.repository;

import com.cinehub.movie.entity.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends MongoRepository<Movie, String> {
    
    List<Movie> findByGenresContaining(String genre); //only 1 genre
    
    List<Movie> findByReleaseDate(LocalDate releaseDate);
    
    List<Movie> findByDirector(String director);

    List<Movie> findByStatus(String status);
    
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }") //ignore capitalization
    List<Movie> findByTitle(String title);
    
    @Query("{ 'rating': { $gte: ?0 } }")
    List<Movie> findByRatingGreaterThanEqual(Double minRating);
}