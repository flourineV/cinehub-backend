package com.cinehub.auth.scheduler;

import com.cinehub.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final PasswordResetService passwordResetService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanExpiredOtps() {
        log.info("[Scheduler] Cleaning expired OTP tokens...");
        passwordResetService.deleteExpiredOtps();
    }
}
