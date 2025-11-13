package com.cinehub.movie.controller;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.response.PagedResponse;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.security.AuthChecker;
import com.cinehub.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

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
    public ResponseEntity<List<MovieSummaryResponse>> searchMovies(
            @RequestParam String keyword) {
        List<MovieSummaryResponse> movies = movieService.searchMovies(keyword);
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

    @GetMapping
    public ResponseEntity<PagedResponse<MovieSummaryResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortType) {
        AuthChecker.requireManagerOrAdmin();
        PagedResponse<MovieSummaryResponse> pages = movieService.adminSearch(keyword, status, page, size, sortBy,
                sortType);
        return ResponseEntity.ok(pages);
    }

    public ResponseEntity<Void> changeStatus(@PathVariable UUID id,
            @RequestBody ChangeStatusRequest req) {
        AuthChecker.requireManagerOrAdmin();
        movieService.changeStatus(id, req.status());
        return ResponseEntity.noContent().build();
    }

    public static record ChangeStatusRequest(MovieStatus status) {
    }
}
