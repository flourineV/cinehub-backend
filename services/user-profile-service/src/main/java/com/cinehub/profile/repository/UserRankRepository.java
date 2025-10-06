package com.cinehub.profile.repository;

import com.cinehub.profile.entity.UserRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRankRepository extends JpaRepository<UserRank, UUID> {

    Optional<UserRank> findByName(String name);

    // Tìm Rank mặc định (min = 0)
    Optional<UserRank> findByMinPoints(Integer minPoints);

    // Tìm Rank tốt nhất theo điểm số hiện tại của người dùng
    @Query("SELECT r FROM UserRank r WHERE r.minPoints <= :points ORDER BY r.minPoints DESC LIMIT 1")
    Optional<UserRank> findBestRankByPoints(Integer points);
}