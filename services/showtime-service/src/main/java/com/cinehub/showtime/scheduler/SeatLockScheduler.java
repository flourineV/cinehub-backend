package com.cinehub.showtime.scheduler;

import com.cinehub.showtime.events.SeatUnlockedEvent;
import com.cinehub.showtime.producer.ShowtimeProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final StringRedisTemplate redisTemplate;
    private final ShowtimeProducer showtimeProducer;

    /**
     * Smart scheduler – tự quản lý TTL logic.
     * Redis value được lưu dạng: bookingId|expireAtEpochMillis
     */
    @Scheduled(fixedRate = 5000) // chạy mỗi 5s
    public void checkExpiredLocks() {
        Set<String> keys = redisTemplate.keys("seat:*");
        if (keys == null || keys.isEmpty())
            return;

        for (String key : keys) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value == null || !value.contains("|"))
                    continue;

                // Parse expireAt từ value (bookingId|expireAt)
                String[] parts = value.split("\\|");
                long expireAt = Long.parseLong(parts[1]);

                if (System.currentTimeMillis() > expireAt) {
                    // Key đã hết hạn → xóa và gửi event
                    redisTemplate.delete(key);

                    String[] idParts = key.split(":");
                    UUID showtimeId = UUID.fromString(idParts[1]);
                    UUID seatId = UUID.fromString(idParts[2]);

                    SeatUnlockedEvent event = new SeatUnlockedEvent(
                            showtimeId,
                            List.of(seatId),
                            "timeout");
                    showtimeProducer.sendSeatUnlockedEvent(event);
                    log.info("⏱️ Seat {} of showtime {} auto-unlocked (timeout)", seatId, showtimeId);
                }

            } catch (Exception e) {
                log.error("❌ Error parsing Redis key {}: {}", key, e.getMessage());
            }
        }
    }
}
