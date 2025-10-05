package com.cinehub.profile.repository;

import com.cinehub.profile.entity.UserFavoriteMovie;
import com.cinehub.profile.entity.FavoriteMovieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFavoriteMovieRepository extends JpaRepository<UserFavoriteMovie, FavoriteMovieId> {

    List<UserFavoriteMovie> findByUser_Id(UUID userId);

    boolean existsById_UserIdAndId_TmdbId(UUID userId, Integer tmdbId);

    void deleteById_UserIdAndId_TmdbId(UUID userId, Integer tmdbId);
}
