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
    private final com.cinehub.profile.repository.UserProfileRepository userProfileRepository;

    public FavoriteMovieResponse addFavorite(FavoriteMovieRequest request) {
        FavoriteMovieId id = new FavoriteMovieId(request.getUserId(), request.getMovieId());

        if (favoriteMovieRepository.existsById_UserIdAndId_MovieId(request.getUserId(), request.getMovieId())) {
            throw new RuntimeException("Movie already in favorites");
        }

        // Load UserProfile entity
        com.cinehub.profile.entity.UserProfile userProfile = userProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User profile not found for userId: " + request.getUserId()));

        UserFavoriteMovie favorite = UserFavoriteMovie.builder()
                .id(id)
                .user(userProfile)
                .build();

        return mapToResponse(favoriteMovieRepository.save(favorite));
    }

    public List<FavoriteMovieResponse> getFavoritesByUser(UUID userId) {
        return favoriteMovieRepository.findById_UserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void removeFavorite(UUID userId, UUID movieId) {
        if (!favoriteMovieRepository.existsById_UserIdAndId_MovieId(userId, movieId)) {
            throw new ResourceNotFoundException("Favorite movie not found for userId: " + userId);
        }
        favoriteMovieRepository.deleteById_UserIdAndId_MovieId(userId, movieId);
    }

    public boolean isFavorite(UUID userId, UUID movieId) {
        return favoriteMovieRepository.existsById_UserIdAndId_MovieId(userId, movieId);
    }

    private FavoriteMovieResponse mapToResponse(UserFavoriteMovie entity) {
        if (entity == null)
            return null;

        return FavoriteMovieResponse.builder()
                .movieId(entity.getId().getMovieId())
                .addedAt(entity.getAddedAt())
                .build();
    }
}
