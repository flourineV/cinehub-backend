package com.cinehub.movie.repository;

import com.cinehub.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByGenre(String genre);
    
    List<Movie> findByReleaseYear(Integer releaseYear);
    
    List<Movie> findByDirector(String director);
    
    @Query("SELECT m FROM Movie m WHERE m.title LIKE %:title%")
    List<Movie> findByTitleContaining(@Param("title") String title);
    
    @Query("SELECT m FROM Movie m WHERE m.rating >= :minRating")
    List<Movie> findByRatingGreaterThanEqual(@Param("minRating") Double minRating);
    
    Optional<Movie> findByTitleAndReleaseYear(String title, Integer releaseYear);
}
