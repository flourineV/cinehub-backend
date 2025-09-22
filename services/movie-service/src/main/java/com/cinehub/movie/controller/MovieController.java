package com.cinehub.movie.controller;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
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
            Pageable pageable
    ) {
        Page<MovieSummaryResponse> movies = movieService.searchMovies(title, pageable);
        return ResponseEntity.ok(movies);
    }

    // ================== DETAIL ==================
    @GetMapping("/{tmdbId}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@PathVariable Integer tmdbId) {
        MovieDetailResponse movie = movieService.getMovieDetail(tmdbId);
        return ResponseEntity.ok(movie);
    }
}
