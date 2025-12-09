package com.cinehub.profile.repository;

import com.cinehub.profile.entity.UserFavoriteMovie;
import com.cinehub.profile.entity.FavoriteMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFavoriteMovieRepository extends JpaRepository<UserFavoriteMovie, FavoriteMovieId> {

    List<UserFavoriteMovie> findById_UserId(UUID userId);

    boolean existsById_UserIdAndId_MovieId(UUID userId, UUID movieId);

    void deleteById_UserIdAndId_MovieId(UUID userId, UUID movieId);
}
