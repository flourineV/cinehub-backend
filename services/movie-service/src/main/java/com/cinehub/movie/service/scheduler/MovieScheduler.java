package com.cinehub.movie.service.scheduler;

import com.cinehub.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieScheduler {

    private final MovieService movieService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoSyncMovies() {
        log.info("=== [SCHEDULER] Bắt đầu đồng bộ phim tự động ===");
        try {
            movieService.syncMovies();
            log.info("=== [SCHEDULER] Đồng bộ phim thành công ===");
        } catch (Exception e) {
            log.error("[SCHEDULER] Lỗi khi đồng bộ phim: {}", e.getMessage(), e);
        }
    }
}
