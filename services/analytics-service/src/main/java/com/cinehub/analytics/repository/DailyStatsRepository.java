package com.cinehub.analytics.repository;

import com.cinehub.analytics.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, UUID> {

    Optional<DailyStats> findByDate(LocalDate date);

    List<DailyStats> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
}
