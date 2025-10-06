package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.FavoriteMovieRequest;
import com.cinehub.profile.dto.response.FavoriteMovieResponse;
import com.cinehub.profile.service.UserFavoriteMovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profiles/favorites")
@RequiredArgsConstructor
public class UserFavoriteMovieController {

    private final UserFavoriteMovieService favoriteMovieService;

    @PostMapping
    public ResponseEntity<FavoriteMovieResponse> addFavorite(@Valid @RequestBody FavoriteMovieRequest request) {
        return ResponseEntity.ok(favoriteMovieService.addFavorite(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<FavoriteMovieResponse>> getFavorites(@PathVariable UUID userId) {
        return ResponseEntity.ok(favoriteMovieService.getFavoritesByUser(userId));
    }

    @DeleteMapping("/{userId}/{tmdbId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID userId, @PathVariable Integer tmdbId) {
        favoriteMovieService.removeFavorite(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }
}
