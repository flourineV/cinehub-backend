package com.cinehub.movie.controller;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.security.AuthChecker;
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
public class MovieController {

    private final MovieService movieService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncMovies() {
        AuthChecker.requireManagerOrAdmin();
        movieService.syncMovies();
        return ResponseEntity.ok("Movies synced successfully!");
    }

    @GetMapping("/now-playing")
    public ResponseEntity<Page<MovieSummaryResponse>> getNowPlaying(Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.getNowPlayingMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<MovieSummaryResponse>> getUpcoming(Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.getUpcomingMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/archived")
    public ResponseEntity<Page<MovieSummaryResponse>> getArchivedMovies(Pageable pageable) {
        AuthChecker.requireManagerOrAdmin();
        Page<MovieSummaryResponse> movies = movieService.getArchivedMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MovieSummaryResponse>> searchMovies(
            @RequestParam String title,
            Pageable pageable) {
        Page<MovieSummaryResponse> movies = movieService.searchMovies(title, pageable);
        return ResponseEntity.ok(movies);
    }

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
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable UUID id) {
        AuthChecker.requireManagerOrAdmin();
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
