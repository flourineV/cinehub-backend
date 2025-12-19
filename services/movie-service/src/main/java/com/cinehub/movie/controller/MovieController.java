package com.cinehub.movie.controller;

import com.cinehub.movie.dto.BulkAddMoviesRequest;
import com.cinehub.movie.dto.BulkAddMoviesResponse;
import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.response.PagedResponse;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.scheduler.MovieStatusScheduler;
import com.cinehub.movie.security.AuthChecker;
import com.cinehub.movie.security.InternalAuthChecker;
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
    private final InternalAuthChecker internalAuthChecker;

    @PostMapping("/bulk-from-tmdb")
    public ResponseEntity<BulkAddMoviesResponse> bulkAddMoviesFromTmdb(
            @Valid @RequestBody BulkAddMoviesRequest request) {
        AuthChecker.requireManagerOrAdmin();
        BulkAddMoviesResponse response = movieService.bulkAddMoviesFromTmdb(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/now-playing")
    public ResponseEntity<Page<MovieSummaryResponse>> getNowPlaying(
            Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language) {
        Page<MovieSummaryResponse> movies = movieService.getNowPlayingMovies(pageable, language);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<MovieSummaryResponse>> getUpcoming(
            Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language) {
        Page<MovieSummaryResponse> movies = movieService.getUpcomingMovies(pageable, language);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/archived")
    public ResponseEntity<Page<MovieSummaryResponse>> getArchivedMovies(Pageable pageable) {
        AuthChecker.requireManagerOrAdmin();
        Page<MovieSummaryResponse> movies = movieService.getArchivedMovies(pageable);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/tmdb/{tmdbId}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(
            @PathVariable Integer tmdbId,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language) {
        MovieDetailResponse movie = movieService.getMovieDetail(tmdbId, language);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieByUuid(
            @PathVariable UUID id,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language) {
        MovieDetailResponse response = movieService.getMovieByUuid(id, language);
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

    @GetMapping("/search") // dành cho user tìm kiếm
    public ResponseEntity<List<MovieSummaryResponse>> searchMovies(
            @RequestParam String keyword) {
        List<MovieSummaryResponse> movies = movieService.searchMovies(keyword);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/advanced-search")
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

    /**
     * Internal API: Set movie to NOW_PLAYING when showtime is created
     */
    @PostMapping("/{id}/set-now-playing")
    public ResponseEntity<Void> setMovieToNowPlaying(
            @PathVariable UUID id,
            @RequestHeader("X-Internal-Secret") String internalSecret) {
        internalAuthChecker.requireInternal(internalSecret);
        movieService.changeStatus(id, MovieStatus.NOW_PLAYING);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/titles")
    public ResponseEntity<java.util.Map<UUID, String>> getBatchMovieTitles(
            @RequestBody List<UUID> movieIds,
            @RequestHeader("X-Internal-Secret") String internalSecret) {
        internalAuthChecker.requireInternal(internalSecret);
        java.util.Map<UUID, String> titles = movieService.getBatchMovieTitles(movieIds);
        return ResponseEntity.ok(titles);
    }

    public static record ChangeStatusRequest(MovieStatus status) {
    }

    public static record UpdateStatusResponse(
            int upcomingToNowPlaying,
            int nowPlayingToArchived,
            String message) {
    }
}
