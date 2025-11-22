package com.cinehub.movie.controller;

import com.cinehub.movie.dto.AddMovieFromTmdbRequest;
import com.cinehub.movie.dto.BulkAddMoviesRequest;
import com.cinehub.movie.dto.BulkAddMoviesResponse;
import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.response.PagedResponse;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.scheduler.MovieStatusScheduler;
import com.cinehub.movie.security.AuthChecker;
import com.cinehub.movie.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieStatusScheduler movieStatusScheduler;

    @PostMapping("/admin/sync")
    public ResponseEntity<String> syncMovies() {
        AuthChecker.requireManagerOrAdmin();
        movieService.syncMovies();
        return ResponseEntity.ok("Movies synced successfully!");
    }

    @PostMapping("/from-tmdb")
    public ResponseEntity<MovieDetailResponse> addMovieFromTmdb(
            @Valid @RequestBody AddMovieFromTmdbRequest request) {
        AuthChecker.requireManagerOrAdmin();
        MovieDetailResponse response = movieService.addMovieFromTmdb(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk-from-tmdb")
    public ResponseEntity<BulkAddMoviesResponse> bulkAddMoviesFromTmdb(
            @Valid @RequestBody BulkAddMoviesRequest request) {
        AuthChecker.requireManagerOrAdmin();
        BulkAddMoviesResponse response = movieService.bulkAddMoviesFromTmdb(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @RequestParam(required = false) String genres,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortType) {
        AuthChecker.requireManagerOrAdmin();
        PagedResponse<MovieSummaryResponse> pages = movieService.adminSearch(keyword, status, genres, page, size,
                sortBy,
                sortType);
        return ResponseEntity.ok(pages);
    }

    public ResponseEntity<Void> changeStatus(@PathVariable UUID id,
            @RequestBody ChangeStatusRequest req) {
        AuthChecker.requireManagerOrAdmin();
        movieService.changeStatus(id, req.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available-for-range")
    public ResponseEntity<List<MovieSummaryResponse>> getAvailableMoviesForDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MovieSummaryResponse> movies = movieService.getAvailableMoviesForDateRange(startDate, endDate);
        return ResponseEntity.ok(movies);
    }

    @PostMapping("/update-status")
    public ResponseEntity<UpdateStatusResponse> updateMovieStatuses() {
        AuthChecker.requireManagerOrAdmin();
        MovieStatusScheduler.UpdateStatusResult result = movieStatusScheduler.updateAllMovieStatuses();
        return ResponseEntity.ok(new UpdateStatusResponse(
                result.getUpcomingToNowPlaying(),
                result.getNowPlayingToArchived(),
                "Movie statuses updated successfully"));
    }

    @PostMapping("/{id}/set-now-playing")
    public ResponseEntity<Void> setMovieNowPlaying(@PathVariable UUID id) {
        // Internal endpoint - no auth required (called by showtime-service)
        movieService.setMovieNowPlaying(id);
        return ResponseEntity.ok().build();
    }

    public static record ChangeStatusRequest(MovieStatus status) {
    }

    public static record UpdateStatusResponse(
            int upcomingToNowPlaying,
            int nowPlayingToArchived,
            String message) {
    }
}
