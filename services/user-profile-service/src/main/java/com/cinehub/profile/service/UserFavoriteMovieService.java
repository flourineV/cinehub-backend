package com.cinehub.profile.service;

import com.cinehub.profile.dto.request.FavoriteMovieRequest;
import com.cinehub.profile.dto.response.FavoriteMovieResponse;
import com.cinehub.profile.entity.FavoriteMovieId;
import com.cinehub.profile.entity.UserFavoriteMovie;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.UserFavoriteMovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFavoriteMovieService {

    private final UserFavoriteMovieRepository favoriteMovieRepository;

    public FavoriteMovieResponse addFavorite(FavoriteMovieRequest request) {
        FavoriteMovieId id = new FavoriteMovieId(request.getUserId(), request.getTmdbId());

        if (favoriteMovieRepository.existsById_UserIdAndId_TmdbId(request.getUserId(), request.getTmdbId())) {
            throw new RuntimeException("Movie already in favorites");
        }

        UserFavoriteMovie favorite = UserFavoriteMovie.builder()
                .id(id)
                .build();

        return mapToResponse(favoriteMovieRepository.save(favorite));
    }

    public List<FavoriteMovieResponse> getFavoritesByUser(UUID userId) {
        return favoriteMovieRepository.findByUser_Id(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void removeFavorite(UUID userId, Integer tmdbId) {
        if (!favoriteMovieRepository.existsById_UserIdAndId_TmdbId(userId, tmdbId)) {
            throw new ResourceNotFoundException("Favorite movie not found for userId: " + userId);
        }
        favoriteMovieRepository.deleteById_UserIdAndId_TmdbId(userId, tmdbId);
    }

    private FavoriteMovieResponse mapToResponse(UserFavoriteMovie entity) {
        if (entity == null)
            return null;

        return FavoriteMovieResponse.builder()
                .tmdbId(entity.getId().getTmdbId())
                .addedAt(entity.getAddedAt())
                .build();
    }
}
