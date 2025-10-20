package com.cinehub.movie.controller;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*", maxAge = 3600)
public class MovieController {

    private final MovieService movieService;

    // ================== SYNC ==================
    @PostMapping("/sync")
    public ResponseEntity<String> syncMovies() {
        movieService.syncMovies();
        return ResponseEntity.ok("Movies synced successfully!");
    }

    // ================== LIST NOW PLAYING ==================
    @GetMapping("/now-playing")
    public ResponseEntity<Page<MovieSummaryResponse>> getNowPlaying(Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.getNowPlayingMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    // ================== LIST UPCOMING ==================
    @GetMapping("/upcoming")
    public ResponseEntity<Page<MovieSummaryResponse>> getUpcoming(Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.getUpcomingMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    // ================== SEARCH ==================
    @GetMapping("/search")
    public ResponseEntity<Page<MovieSummaryResponse>> searchMovies(
            @RequestParam String title,
            Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.searchMovies(title, pageable);
        return ResponseEntity.ok(movies);
    }

    // ================== DETAIL ==================
    @GetMapping("/tmdb/{tmdbId}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@PathVariable Integer tmdbId) {
        MovieDetailResponse movie = movieService.getMovieDetail(tmdbId);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieByUuid(@PathVariable UUID id) {
        MovieDetailResponse response = movieService.getMovieByUuid(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> updateMovie(
            @PathVariable UUID id,
            @RequestBody MovieDetailResponse request) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable UUID id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
