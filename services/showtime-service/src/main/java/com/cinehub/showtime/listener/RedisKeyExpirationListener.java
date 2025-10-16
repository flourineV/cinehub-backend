// Trong Showtime Service: com.cinehub.showtime.listener.RedisKeyExpirationListener.java

package com.cinehub.showtime.listener;

import com.cinehub.showtime.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyExpirationListener {

    private final SeatLockService seatLockService;

    /**
     * @param expiredKey: Key đã hết hạn (e.g., "seat:<showtimeId>:<seatId>")
     */
    public void handleExpiredKey(String expiredKey) {
        if (expiredKey.startsWith("seat:")) {
            log.warn("🚨 Lock key expired: {}", expiredKey);

            try {
                // Key: [seat, showtimeId, seatId]
                String[] parts = expiredKey.split(":");
                if (parts.length < 3)
                    return;

                UUID showtimeId = UUID.fromString(parts[1]);
                UUID seatId = UUID.fromString(parts[2]);

                // Xử lý giải phóng ghế
                seatLockService.handleExpiredLock(showtimeId, seatId);
            } catch (Exception e) {
                log.error("Error processing expired key {}: {}", expiredKey, e.getMessage());
            }
        }
    }
}