package com.cinehub.movie.scheduler;

import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.repository.MovieSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieStatusScheduler {

    private final MovieSummaryRepository movieSummaryRepository;

    /**
     * Scheduled task to update movie status based on endDate only
     * startDate logic is handled when showtimes are created
     * Runs every day at 00:05 AM
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void updateMovieStatuses() {
        log.info("Starting scheduled movie status update...");

        UpdateStatusResult result = updateAllMovieStatuses();

        log.info("Scheduled movie status update completed: {} NOW_PLAYING→ARCHIVED",
                result.getNowPlayingToArchived());
    }

    /**
     * Public method to manually trigger status update (can be called from API)
     * Only handles NOW_PLAYING → ARCHIVED transition
     * UPCOMING → NOW_PLAYING is handled when showtimes are created
     * 
     * @return UpdateStatusResult with counts of status changes
     */
    public UpdateStatusResult updateAllMovieStatuses() {
        LocalDate today = LocalDate.now();

        AtomicInteger nowPlayingToArchived = new AtomicInteger(0);

        // Update NOW_PLAYING → ARCHIVED (when endDate < today)
        List<MovieSummary> nowPlayingMovies = movieSummaryRepository.findByStatus(MovieStatus.NOW_PLAYING);
        nowPlayingMovies.forEach(movie -> {
            if (movie.getEndDate() != null && movie.getEndDate().isBefore(today)) {
                movie.setStatus(MovieStatus.ARCHIVED);
                nowPlayingToArchived.incrementAndGet();
                movieSummaryRepository.save(movie);
                log.debug("Movie {} changed from NOW_PLAYING to ARCHIVED", movie.getTitle());
            }
        });

        return new UpdateStatusResult(0, nowPlayingToArchived.get());
    }

    public static class UpdateStatusResult {
        private final int upcomingToNowPlaying;
        private final int nowPlayingToArchived;

        public UpdateStatusResult(int upcomingToNowPlaying, int nowPlayingToArchived) {
            this.upcomingToNowPlaying = upcomingToNowPlaying;
            this.nowPlayingToArchived = nowPlayingToArchived;
        }

        public int getUpcomingToNowPlaying() {
            return upcomingToNowPlaying;
        }

        public int getNowPlayingToArchived() {
            return nowPlayingToArchived;
        }
    }
}
