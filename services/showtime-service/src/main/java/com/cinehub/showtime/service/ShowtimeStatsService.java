package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.response.ShowtimeStatsResponse;
import com.cinehub.showtime.entity.ShowtimeStatus;
import com.cinehub.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowtimeStatsService {

    private final ShowtimeRepository showtimeRepository;

    public ShowtimeStatsResponse getOverview(UUID theaterId) {
        LocalDateTime now = LocalDateTime.now();
        
        long total;
        long nowPlaying;
        long upcoming;
        long suspended;

        if (theaterId != null) {
            // Query by theater - use optimized count queries
            total = showtimeRepository.countByTheaterId(theaterId);
            nowPlaying = showtimeRepository.countNowPlayingByTheaterId(theaterId, ShowtimeStatus.ACTIVE, now);
            upcoming = showtimeRepository.countUpcomingByTheaterId(theaterId, ShowtimeStatus.ACTIVE, now);
            suspended = showtimeRepository.countByTheaterIdAndStatus(theaterId, ShowtimeStatus.SUSPENDED);
        } else {
            // Query all - use optimized count queries
            total = showtimeRepository.count();
            nowPlaying = showtimeRepository.countNowPlaying(ShowtimeStatus.ACTIVE, now);
            upcoming = showtimeRepository.countUpcoming(ShowtimeStatus.ACTIVE, now);
            suspended = showtimeRepository.countByStatus(ShowtimeStatus.SUSPENDED);
        }

        return ShowtimeStatsResponse.builder()
                .totalShowtimes(total)
                .activeShowtimes(nowPlaying)
                .upcomingShowtimes(upcoming)
                .suspendedShowtimes(suspended)
                .build();
    }
}
