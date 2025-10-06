package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.TheaterRequest;
import com.cinehub.showtime.dto.response.TheaterResponse;
import com.cinehub.showtime.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/showtimes/theaters")
@RequiredArgsConstructor

public class TheaterController {

    private final TheaterService theaterService;

    @PostMapping
    public ResponseEntity<TheaterResponse> createTheater(@RequestBody TheaterRequest request) {
        TheaterResponse response = theaterService.createTheater(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<TheaterResponse> getTheaterById(@PathVariable UUID id) {
        TheaterResponse response = theaterService.getTheaterById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TheaterResponse>> getAllTheaters() {
        List<TheaterResponse> responseList = theaterService.getAllTheaters();
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TheaterResponse>> getTheatersByProvince(@RequestParam UUID provinceId) {
        List<TheaterResponse> responseList = theaterService.getTheatersByProvince(provinceId);
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TheaterResponse> updateTheater(
            @PathVariable UUID id,
            @RequestBody TheaterRequest request) {

        TheaterResponse response = theaterService.updateTheater(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheater(@PathVariable UUID id) {
        theaterService.deleteTheater(id);
        return ResponseEntity.noContent().build();
    }
}