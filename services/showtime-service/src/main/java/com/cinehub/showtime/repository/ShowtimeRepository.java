package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
