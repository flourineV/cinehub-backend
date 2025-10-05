package com.cinehub.profile.repository;

import com.cinehub.profile.entity.UserRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRankRepository extends JpaRepository<UserRank, UUID> {

    // 1. Tìm Rank theo tên (Đã có, giữ nguyên)
    Optional<UserRank> findByName(String name);

    // 2. Tìm Rank mặc định (Dùng cho createProfile)
    // Rank mặc định luôn là Rank có min_points = 0
    Optional<UserRank> findByMinPoints(Integer minPoints);

    // 3. Tìm Rank tốt nhất theo điểm số hiện tại của người dùng (Dùng cho logic
    // Nâng Rank)
    // Phương thức này tìm Rank có minPoints <= điểm số, sau đó sắp xếp giảm dần
    // để lấy Rank cao nhất mà người dùng đạt được.
    @Query("SELECT r FROM UserRank r WHERE r.minPoints <= :points ORDER BY r.minPoints DESC LIMIT 1")
    Optional<UserRank> findBestRankByPoints(Integer points);
}