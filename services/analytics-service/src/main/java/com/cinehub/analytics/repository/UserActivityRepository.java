package com.cinehub.analytics.repository;

import com.cinehub.analytics.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<UserActivity> findByActivityTypeAndCreatedAtBetween(
            String activityType,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT ua.userId) FROM UserActivity ua WHERE ua.createdAt BETWEEN :start AND :end")
    Long countUniqueVisitors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT ua.activityType, COUNT(ua) FROM UserActivity ua " +
            "WHERE ua.createdAt BETWEEN :start AND :end " +
            "GROUP BY ua.activityType")
    List<Object[]> countByActivityTypeAndDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
