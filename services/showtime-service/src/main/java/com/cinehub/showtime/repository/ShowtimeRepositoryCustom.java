package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public interface ShowtimeRepositoryCustom {
    Page<Showtime> findAvailableShowtimesWithFiltersDynamic(
            UUID provinceId,
            UUID theaterId,
            UUID roomId,
            UUID movieId,
            UUID showtimeId,
            LocalDate selectedDate,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            LocalTime fromTime,
            LocalTime toTime,
            LocalDateTime now,
            Pageable pageable);
}
