package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    List<Showtime> findByTheaterIdAndStartTimeBetween(
            UUID theaterId, LocalDateTime start, LocalDateTime end);

    List<Showtime> findByMovieId(UUID movieId);

    // Tìm showtimes overlap: endTime > newStart AND startTime < newEnd
    @Query("""
            SELECT s FROM Showtime s
            WHERE s.room.id = :roomId
            AND s.endTime > :newStart
            AND s.startTime < :newEnd
            """)
    List<Showtime> findOverlappingShowtimes(
            @Param("roomId") UUID roomId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd);

    // Tìm showtimes trong khoảng thời gian (dùng cho calculateFreeSlots)
    List<Showtime> findByRoomIdAndEndTimeAfterAndStartTimeBefore(
            UUID roomId, LocalDateTime startTime, LocalDateTime endTime);

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
            WHERE s.theater.id = :theaterId
            AND s.startTime >= :now
            ORDER BY s.movieId, s.startTime
            """)
    List<Showtime> findByTheaterIdAndStartTimeAfter(
            @Param("theaterId") UUID theaterId,
            @Param("now") LocalDateTime now);

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

    List<Showtime> findByMovieIdAndTheaterIdAndStartTimeBetween(
            UUID movieId,
            UUID theaterId,
            LocalDateTime start,
            LocalDateTime end);

    List<Showtime> findByMovieIdAndStartTimeBetween(
            UUID movieId,
            LocalDateTime start,
            LocalDateTime end);

    List<Showtime> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end);

}
