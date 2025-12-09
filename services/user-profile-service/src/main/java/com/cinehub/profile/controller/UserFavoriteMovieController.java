package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.FavoriteMovieRequest;
import com.cinehub.profile.dto.response.FavoriteMovieResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.UserFavoriteMovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/favorites")
@RequiredArgsConstructor
public class UserFavoriteMovieController {

    private final UserFavoriteMovieService favoriteMovieService;

    @PostMapping
    public ResponseEntity<FavoriteMovieResponse> addFavorite(@Valid @RequestBody FavoriteMovieRequest request) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(favoriteMovieService.addFavorite(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<FavoriteMovieResponse>> getFavorites(@PathVariable UUID userId) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(favoriteMovieService.getFavoritesByUser(userId));
    }

    @DeleteMapping("/{userId}/{movieId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID userId, @PathVariable UUID movieId) {
        AuthChecker.requireAuthenticated();
        favoriteMovieService.removeFavorite(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{userId}/{movieId}")
    public ResponseEntity<Boolean> isFavorite(@PathVariable UUID userId, @PathVariable UUID movieId) {
        AuthChecker.requireAuthenticated();
        return ResponseEntity.ok(favoriteMovieService.isFavorite(userId, movieId));
    }
}
