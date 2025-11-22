package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
        List<Showtime> findByTheaterIdAndStartTimeBetween(
                        UUID theaterId, LocalDateTime start, LocalDateTime end);

        List<Showtime> findByMovieId(UUID movieId);

        // PHƯƠNG THỨC MỚI: Tìm suất chiếu trùng lịch trong cùng một Room
        List<Showtime> findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                        UUID roomId, LocalDateTime startTime, LocalDateTime endTime);

        /**
         * Advanced search with filters and pagination
         */
        @Query("""
                        SELECT s FROM Showtime s
                        WHERE s.startTime > :now
                        AND (:provinceId IS NULL OR s.theater.province.id = :provinceId)
                        AND (:theaterId IS NULL OR s.theater.id = :theaterId)
                        AND (:roomId IS NULL OR s.room.id = :roomId)
                        AND (:movieId IS NULL OR s.movieId = :movieId)
                        AND (:showtimeId IS NULL OR s.id = :showtimeId)
                        AND (:showDate IS NULL OR DATE(s.startTime) = :showDate)
                        AND (:showTime IS NULL OR function('time', s.startTime) = :showTime)
                        """)
        Page<Showtime> findAvailableShowtimesWithFilters(
                        @Param("now") LocalDateTime now,
                        @Param("provinceId") UUID provinceId,
                        @Param("theaterId") UUID theaterId,
                        @Param("roomId") UUID roomId,
                        @Param("movieId") UUID movieId,
                        @Param("showtimeId") UUID showtimeId,
                        @Param("showDate") LocalDate showDate,
                        @Param("showTime") LocalTime showTime,
                        Pageable pageable);

        /**
         * Find showtimes by movie and province
         */
        @Query("""
                        SELECT s FROM Showtime s
                        WHERE s.movieId = :movieId
                        AND s.theater.province.id = :provinceId
                        AND s.startTime >= :now
                        ORDER BY s.theater.name, s.startTime
                        """)
        List<Showtime> findByMovieAndProvince(
                        @Param("movieId") UUID movieId,
                        @Param("provinceId") UUID provinceId,
                        @Param("now") LocalDateTime now);
}
