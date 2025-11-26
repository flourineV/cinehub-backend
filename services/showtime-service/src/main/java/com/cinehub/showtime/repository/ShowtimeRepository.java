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

        List<Showtime> findByMovieIdAndStatusAndStartTimeAfter(
                        UUID movieId,
                        com.cinehub.showtime.entity.ShowtimeStatus status,
                        LocalDateTime startTime);

        @Query("""
                            SELECT s FROM Showtime s
                            WHERE s.startTime >= :start AND s.endTime <= :end
                            AND NOT EXISTS (
                                SELECT ss FROM ShowtimeSeat ss
                                WHERE ss.showtime.id = s.id
                            )
                        """)
        List<Showtime> findShowtimesWithoutSeats(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
