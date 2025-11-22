package com.cinehub.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockRedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Validate guest session owns the seats
     * Redis key format: seat:{showtimeId}:{seatId}
     * Redis value format: GUEST|{guestSessionId}|{expireAt}
     */
    public boolean validateGuestSessionOwnsSeats(UUID showtimeId, List<UUID> seatIds, UUID guestSessionId) {
        for (UUID seatId : seatIds) {
            String key = "seat:" + showtimeId + ":" + seatId;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.warn("Seat lock not found for seat {} in showtime {}", seatId, showtimeId);
                return false;
            }
            
            // Parse value: GUEST|{guestSessionId}|{expireAt}
            String[] parts = value.split("\\|");
            if (parts.length < 2) {
                log.warn("Invalid lock value format for seat {}: {}", seatId, value);
                return false;
            }
            
            String ownerType = parts[0];
            String ownerIdentifier = parts[1];
            
            if (!"GUEST".equals(ownerType) || !ownerIdentifier.equals(guestSessionId.toString())) {
                log.warn("Guest session {} does not own seat {} for showtime {} (owner: {}|{})", 
                    guestSessionId, seatId, showtimeId, ownerType, ownerIdentifier);
                return false;
            }
        }
        return true;
    }

    /**
     * Validate user owns the seats
     * Redis key format: seat:{showtimeId}:{seatId}
     * Redis value format: USER|{userId}|{expireAt}
     */
    public boolean validateUserOwnsSeats(UUID showtimeId, List<UUID> seatIds, UUID userId) {
        for (UUID seatId : seatIds) {
            String key = "seat:" + showtimeId + ":" + seatId;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.warn("Seat lock not found for seat {} in showtime {}", seatId, showtimeId);
                return false;
            }
            
            // Parse value: USER|{userId}|{expireAt}
            String[] parts = value.split("\\|");
            if (parts.length < 2) {
                log.warn("Invalid lock value format for seat {}: {}", seatId, value);
                return false;
            }
            
            String ownerType = parts[0];
            String ownerIdentifier = parts[1];
            
            if (!"USER".equals(ownerType) || !ownerIdentifier.equals(userId.toString())) {
                log.warn("User {} does not own seat {} for showtime {} (owner: {}|{})", 
                    userId, seatId, showtimeId, ownerType, ownerIdentifier);
                return false;
            }
        }
        return true;
    }
}
