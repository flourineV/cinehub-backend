package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.BatchShowtimeRequest;
import com.cinehub.showtime.dto.request.ShowtimeRequest;
import com.cinehub.showtime.dto.request.ValidateShowtimeRequest;
import com.cinehub.showtime.dto.response.AutoGenerateShowtimesResponse;
import com.cinehub.showtime.dto.response.BatchShowtimeResponse;
import com.cinehub.showtime.dto.response.PagedResponse;
import com.cinehub.showtime.dto.response.ShowtimeConflictResponse;
import com.cinehub.showtime.dto.response.ShowtimeDetailResponse;
import com.cinehub.showtime.dto.response.ShowtimeResponse;
import com.cinehub.showtime.dto.response.ShowtimesByMovieResponse;
import com.cinehub.showtime.dto.response.TheaterShowtimesResponse;
import com.cinehub.showtime.security.AuthChecker;
import com.cinehub.showtime.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(@RequestBody ShowtimeRequest request) {
        AuthChecker.requireManagerOrAdmin();
        ShowtimeResponse response = showtimeService.createShowtime(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchShowtimeResponse> createShowtimesBatch(@RequestBody BatchShowtimeRequest request) {
        AuthChecker.requireManagerOrAdmin();
        BatchShowtimeResponse response = showtimeService.createShowtimesBatch(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@PathVariable UUID id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getAllShowtimes() {
        return ResponseEntity.ok(showtimeService.getAllShowtimes());
    }

    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<ShowtimesByMovieResponse> getShowtimesByMovie(@PathVariable UUID movieId) {
        return ResponseEntity.ok(showtimeService.getShowtimesByMovieGrouped(movieId));
    }

    @GetMapping("/by-theater")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByTheaterAndDate(
            @RequestParam UUID theaterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(showtimeService.getShowtimesByTheaterAndDate(theaterId, startDate, endDate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(@PathVariable UUID id,
            @RequestBody ShowtimeRequest request) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(showtimeService.updateShowtime(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable UUID id) {
        AuthChecker.requireManagerOrAdmin();
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<PagedResponse<ShowtimeDetailResponse>> getAvailableShowtimesForGuest(
            @RequestParam(required = false) UUID provinceId,
            @RequestParam(required = false) UUID theaterId,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime showTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortType) {
        PagedResponse<ShowtimeDetailResponse> response = showtimeService.getAllAvailableShowtimes(
                provinceId, theaterId, null, movieId, null, showDate, showTime, page, size, sortBy, sortType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/available")
    public ResponseEntity<PagedResponse<ShowtimeDetailResponse>> getAllAvailableShowtimesForAdmin(
            @RequestParam(required = false) UUID provinceId,
            @RequestParam(required = false) UUID theaterId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) UUID showtimeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime showTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortType) {
        AuthChecker.requireManagerOrAdmin();
        PagedResponse<ShowtimeDetailResponse> response = showtimeService.getAllAvailableShowtimes(
                provinceId, theaterId, roomId, movieId, showtimeId, showDate, showTime, page, size, sortBy, sortType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<ShowtimeConflictResponse> validateShowtime(@RequestBody ValidateShowtimeRequest request) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(showtimeService.validateShowtime(request));
    }

    @GetMapping("/by-room")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByRoomAndDateRange(
            @RequestParam UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        AuthChecker.requireManagerOrAdmin();
        return ResponseEntity.ok(showtimeService.getShowtimesByRoomAndDateRange(roomId, start, end));
    }

    @PostMapping("/auto-generate")
    public ResponseEntity<AutoGenerateShowtimesResponse> autoGenerateShowtimes(@RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        AuthChecker.requireManagerOrAdmin();
        AutoGenerateShowtimesResponse response = showtimeService.autoGenerateShowtimes(startDate, endDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-movie-and-province")
    public ResponseEntity<List<TheaterShowtimesResponse>> getTheaterShowtimesByMovieAndProvince(
            @RequestParam UUID movieId,
            @RequestParam UUID provinceId) {
        List<TheaterShowtimesResponse> response = showtimeService
                .getTheaterShowtimesByMovieAndProvince(movieId, provinceId);
        return ResponseEntity.ok(response);
    }
}
