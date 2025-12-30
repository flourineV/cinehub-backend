package com.cinehub.auth.config;

import com.cinehub.auth.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final OtpService otpService;

    // Cleanup expired OTPs every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void cleanupExpiredOtps() {
        otpService.cleanupExpiredOtps();
    }
}