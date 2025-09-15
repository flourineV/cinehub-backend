package com.cinehub.movie.service;

import com.cinehub.movie.entity.Movie;
import com.cinehub.movie.entity.Movie.Rating;
import com.cinehub.movie.entity.Movie.Review;
import com.cinehub.movie.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(String id) {
        return movieRepository.findById(id);
    }

    public List<Movie> getByStatus(String status) {
        return movieRepository.findByStatus(status);
    }

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public Optional<Movie> updateMovie(String id, Movie movieDetails) {
        return movieRepository.findById(id).map(movie -> {
            movie.setTitle(movieDetails.getTitle());
            movie.setDescription(movieDetails.getDescription());
            movie.setDuration(movieDetails.getDuration());
            movie.setReleaseDate(movieDetails.getReleaseDate());
            movie.setStatus(movieDetails.getStatus());
            movie.setLanguage(movieDetails.getLanguage());
            movie.setGenres(movieDetails.getGenres());
            movie.setDirector(movieDetails.getDirector());
            movie.setCast(movieDetails.getCast());
            movie.setReleaseDate(movieDetails.getReleaseDate());
            movie.setRating(movieDetails.getRating());
            movie.setPosterUrl(movieDetails.getPosterUrl());
            movie.setTrailerUrl(movieDetails.getTrailerUrl());
            return movieRepository.save(movie);
        });
    }

    public boolean deleteMovie(String id) {
        return movieRepository.findById(id).map(movie -> {
            movieRepository.delete(movie);
            return true;
        }).orElse(false);
    }

    // Search methods
    public List<Movie> findByGenre(String genre) {
        return movieRepository.findByGenre(genre);
    }

    public List<Movie> findByReleaseDate(LocalDate releaseDate) {
        return movieRepository.findByReleaseDate(releaseDate);
    }

    public List<Movie> findByDirector(String director) {
        return movieRepository.findByDirector(director);
    }   

    public List<Movie> findByTitle(String title) {
        return movieRepository.findByTitle(title);
    }

    public List<Movie> findByRatingGreaterThanEqual(Double minRating) {
        return movieRepository.findByRatingGreaterThanEqual(minRating);
    }

    // Add Review & update rating
    public Optional<Movie> addReview(String movieId, Review review) {
        return movieRepository.findById(movieId).map(movie -> {
            review.setCreatedAt(LocalDateTime.now());
            movie.getReviews().add(review);

            // update rating avg + count
            int count = movie.getReviews().size();
            double avg = movie.getReviews().stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0);
            movie.setRating(new Rating(avg, count));
            return movieRepository.save(movie);
        });
    }
}