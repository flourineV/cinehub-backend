package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, String> {
    List<Showtime> findByTheaterIdAndStartTimeBetween(
        String theaterId, LocalDateTime start, LocalDateTime end
    );

    List<Showtime> findByMovieId(String movieId);

    // PHƯƠNG THỨC MỚI: Tìm suất chiếu trùng lịch trong cùng một Room
    List<Showtime> findByRoomIdAndEndTimeAfterAndStartTimeBefore(
        String roomId, LocalDateTime startTime, LocalDateTime endTime
    );
}
