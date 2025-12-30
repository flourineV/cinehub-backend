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
        log.info("Starting movie status update. Today: {}", today);

        AtomicInteger nowPlayingToArchived = new AtomicInteger(0);

        // Update NOW_PLAYING → ARCHIVED (when endDate < today)
        List<MovieSummary> nowPlayingMovies = movieSummaryRepository.findByStatus(MovieStatus.NOW_PLAYING);
        log.info("Found {} NOW_PLAYING movies to check", nowPlayingMovies.size());
        
        nowPlayingMovies.forEach(movie -> {
            log.info("Checking movie: {} (ID: {}) - endDate: {}", 
                    movie.getTitle(), movie.getId(), movie.getEndDate());
            
            if (movie.getEndDate() != null && movie.getEndDate().isBefore(today)) {
                log.info("ARCHIVING movie: {} - endDate {} is before today {}", 
                        movie.getTitle(), movie.getEndDate(), today);
                movie.setStatus(MovieStatus.ARCHIVED);
                nowPlayingToArchived.incrementAndGet();
                movieSummaryRepository.save(movie);
                log.info("Movie {} changed from NOW_PLAYING to ARCHIVED", movie.getTitle());
            } else if (movie.getEndDate() == null) {
                log.warn("Movie {} has NULL endDate, skipping", movie.getTitle());
            } else {
                log.info("Movie {} endDate {} is not before today {}, keeping NOW_PLAYING", 
                        movie.getTitle(), movie.getEndDate(), today);
            }
        });

        log.info("Movie status update completed: {} movies archived", nowPlayingToArchived.get());
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
