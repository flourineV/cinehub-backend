package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.response.ShowtimeStatsResponse;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.ShowtimeStatus;
import com.cinehub.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeStatsService {

    private final ShowtimeRepository showtimeRepository;

    public ShowtimeStatsResponse getOverview(UUID theaterId) {
        List<Showtime> showtimes;

        if (theaterId != null) {
            showtimes = showtimeRepository.findAll().stream()
                    .filter(s -> s.getTheater().getId().equals(theaterId))
                    .collect(Collectors.toList());
        } else {
            showtimes = showtimeRepository.findAll();
        }

        long total = showtimes.size();
        LocalDateTime now = LocalDateTime.now();

        long active = showtimes.stream()
                .filter(s -> s.getStatus() == ShowtimeStatus.ACTIVE && s.getStartTime().isAfter(now))
                .count();

        long suspended = showtimes.stream()
                .filter(s -> s.getStatus() == ShowtimeStatus.SUSPENDED)
                .count();

        long upcoming = showtimes.stream()
                .filter(s -> s.getStartTime().isAfter(now))
                .count();

        return ShowtimeStatsResponse.builder()
                .totalShowtimes(total)
                .activeShowtimes(active)
                .suspendedShowtimes(suspended)
                .upcomingShowtimes(upcoming)
                .build();
    }
}
