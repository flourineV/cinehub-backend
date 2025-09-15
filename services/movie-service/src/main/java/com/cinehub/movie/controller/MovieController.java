package com.cinehub.movie.controller;

import com.cinehub.movie.entity.Movie;
import com.cinehub.movie.entity.Movie.Review;
import com.cinehub.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    // CRUD
   @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable String id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movie> createMovie(@RequestBody Movie movie) {
        return ResponseEntity.ok(movieService.createMovie(movie));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable String id, @RequestBody Movie movieDetails) {
        return movieService.updateMovie(id, movieDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable String id) {
        return movieService.deleteMovie(id) ? 
                ResponseEntity.ok("Movie deleted successfully") : 
                ResponseEntity.notFound().build();
    }

    // Search
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Movie>> findByGenre(@PathVariable String genre) {
        return ResponseEntity.ok(movieService.findByGenres(genre));
    }

    @GetMapping("/release-date/{date}")
    public ResponseEntity<List<Movie>> findByReleaseDate(@PathVariable String date) {
        return ResponseEntity.ok(movieService.findByReleaseDate(LocalDate.parse(date)));
    }

    @GetMapping("/director/{director}")
    public ResponseEntity<List<Movie>> findByDirector(@PathVariable String director) {
        return ResponseEntity.ok(movieService.findByDirector(director));
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<List<Movie>> findByTitle(@PathVariable String title) {
        return ResponseEntity.ok(movieService.findByTitle(title));
    }

    @GetMapping("/rating/{minRating}")
    public ResponseEntity<List<Movie>> findByRatingGreaterThanEqual(@PathVariable Double minRating) {
        return ResponseEntity.ok(movieService.findByRatingGreaterThanEqual(minRating));
    }

    // Review 
    @PostMapping("/{movieId}/reviews")
    public ResponseEntity<Movie> addReview(@PathVariable String movieId, @RequestBody Review review) {
        return movieService.addReview(movieId, review)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
