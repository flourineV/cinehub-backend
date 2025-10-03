package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.response.SeatLockResponse;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private static final int LOCK_TIMEOUT = 300; // 5 phút

    // Lock ghế
    public SeatLockResponse lockSeat(UUID showtimeId, UUID seatId, UUID bookingId) {
        String key = key(showtimeId, seatId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, bookingId.toString(), LOCK_TIMEOUT, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            return SeatLockResponse.builder()
                    .showtimeId(showtimeId)
                    .seatId(seatId)
                    .status("LOCKED")
                    .ttl(LOCK_TIMEOUT)
                    .build();
        } else {
            long ttl = ttl(showtimeId, seatId);
            return SeatLockResponse.builder()
                    .showtimeId(showtimeId)
                    .seatId(seatId)
                    .status("ALREADY_LOCKED")
                    .ttl(Math.max(ttl, 0))
                    .build();
        }
    }

    public SeatLockResponse releaseSeat(UUID showtimeId, UUID seatId) {
        redisTemplate.delete(key(showtimeId, seatId));
        return SeatLockResponse.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status("AVAILABLE")
                .ttl(0)
                .build();
    }

    public SeatLockResponse seatStatus(UUID showtimeId, UUID seatId) {
        boolean locked = isSeatLocked(showtimeId, seatId);
        long ttl = locked ? ttl(showtimeId, seatId) : 0;

        return SeatLockResponse.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status(locked ? "LOCKED" : "AVAILABLE")
                .ttl(ttl)
                .build();
    }

    // ---------- helper methods ----------
    private boolean isSeatLocked(UUID showtimeId, UUID seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(showtimeId, seatId)));
    }

    private long ttl(UUID showtimeId, UUID seatId) {
        Long ttl = redisTemplate.getExpire(key(showtimeId, seatId), TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private String key(UUID showtimeId, UUID seatId) {
        return "seat:" + showtimeId + ":" + seatId;
    }
}
