package com.cinehub.showtime.service;

import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import com.cinehub.showtime.dto.request.SeatSelectionDetail;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import com.cinehub.showtime.dto.request.SeatLockRequest;
import com.cinehub.showtime.events.BookingStatusUpdatedEvent;
import com.cinehub.showtime.events.BookingSeatMappedEvent;
import com.cinehub.showtime.events.SeatUnlockedEvent;
import com.cinehub.showtime.exception.IllegalSeatLockException;
import com.cinehub.showtime.producer.ShowtimeProducer;
import com.cinehub.showtime.websocket.SeatLockWebSocketHandler;
import com.cinehub.showtime.dto.request.SingleSeatLockRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private final ShowtimeProducer showtimeProducer;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatLockWebSocketHandler webSocketHandler;

    @Value("${lock.timeout:200}")
    private int lockTimeout;

    private static final String UPDATE_LOCK_WITH_TTL_SCRIPT = """
            local ttl = redis.call('TTL', KEYS[1])
            if ttl > 0 then
                redis.call('SET', KEYS[1], ARGV[1], 'EX', ttl)
                return 1
            else
                return 0
            end
            """;

    private final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UPDATE_LOCK_WITH_TTL_SCRIPT,
            Long.class);

    @Transactional
    public SeatLockResponse lockSingleSeat(SingleSeatLockRequest req) {

        UUID seatId = req.getSelectedSeat().getSeatId();

        // Validate showtime-seat combination exists
        showtimeSeatRepository.findByShowtime_IdAndSeat_Id(
                req.getShowtimeId(), seatId)
                .orElseThrow(() -> new IllegalSeatLockException(
                        "Showtime or seat not found: showtimeId=" + req.getShowtimeId() + ", seatId=" + seatId));

        String key = key(req.getShowtimeId(), seatId);
        long expireAt = System.currentTimeMillis() + lockTimeout * 1000L;

        String ownerType;
        String ownerIdentifier;

        if (req.getUserId() != null) {
            ownerType = "USER";
            ownerIdentifier = req.getUserId().toString();
        } else {
            ownerType = "GUEST";
            ownerIdentifier = req.getGuestSessionId().toString();
        }

        String value = ownerType + "|" + ownerIdentifier + "|" + expireAt;

        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, lockTimeout, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            throw new IllegalSeatLockException("Seat " + seatId + " is already locked by another user.");
        }

        int updatedCount = showtimeSeatRepository.updateSingleSeatStatus(
                req.getShowtimeId(),
                seatId,
                ShowtimeSeat.SeatStatus.LOCKED,
                LocalDateTime.now());

        log.info("Seat {} locked (Redis+DB) for showtime {} by {}. DB updated: {}",
                seatId, req.getShowtimeId(),
                req.getUserId() != null ? "user " + req.getUserId() : "guest " + req.getGuestSessionId(),
                updatedCount);

        SeatLockResponse response = buildLockResponse(req.getShowtimeId(), seatId, "LOCKED", lockTimeout);

        // Push WebSocket update
        webSocketHandler.broadcastToShowtime(req.getShowtimeId(), response);

        return response;
    }

    @Transactional
    public SeatLockResponse unlockSingleSeat(UUID showtimeId, UUID seatId, UUID userId, UUID guestSessionId) {
        String key = key(showtimeId, seatId);

        String currentValue = redisTemplate.opsForValue().get(key);
        if (currentValue != null) {
            String[] parts = currentValue.split("\\|");
            if (parts.length >= 2) {
                String ownerType = parts[0];
                String ownerIdentifier = parts[1];

                boolean isOwner = false;
                if ("USER".equals(ownerType) && userId != null) {
                    isOwner = ownerIdentifier.equals(userId.toString());
                } else if ("GUEST".equals(ownerType) && guestSessionId != null) {
                    isOwner = ownerIdentifier.equals(guestSessionId.toString());
                }

                if (!isOwner) {
                    throw new IllegalSeatLockException("You don't own this seat lock");
                }
            }
        }

        // Delete Redis lock
        redisTemplate.delete(key);

        // Update DB
        int updatedCount = showtimeSeatRepository.updateSingleSeatStatus(
                showtimeId,
                seatId,
                ShowtimeSeat.SeatStatus.AVAILABLE,
                LocalDateTime.now());

        log.info("Seat {} unlocked (Redis+DB) for showtime {}. DB updated: {}",
                seatId, showtimeId, updatedCount);

        SeatLockResponse response = buildLockResponse(showtimeId, seatId, "AVAILABLE", 0);

        // Push WebSocket update
        webSocketHandler.broadcastToShowtime(showtimeId, response);

        SeatUnlockedEvent event = new SeatUnlockedEvent(
                null, // no bookingId for manual unlock
                showtimeId,
                Collections.singletonList(seatId),
                "Manual unlock by user");
        showtimeProducer.sendSeatUnlockedEvent(event);
        log.info("Published SeatUnlockedEvent for seat {} in showtime {}", seatId, showtimeId);

        return response;
    }

    @Transactional
    public List<SeatLockResponse> lockSeats(SeatLockRequest req) {

        List<SeatLockResponse> responses = new java.util.ArrayList<>();
        List<UUID> successfullyLockedSeats = new java.util.ArrayList<>();
        List<UUID> seatIds = req.getSelectedSeats().stream().map(SeatSelectionDetail::getSeatId).toList();

        for (SeatSelectionDetail seatDetail : req.getSelectedSeats()) {
            UUID seatId = seatDetail.getSeatId();
            String key = key(req.getShowtimeId(), seatId);
            long expireAt = System.currentTimeMillis() + lockTimeout * 1000L;

            String ownerType;
            String ownerIdentifier;

            if (req.getUserId() != null) {
                ownerType = "USER";
                ownerIdentifier = req.getUserId().toString();
            } else {
                ownerType = "GUEST";
                ownerIdentifier = req.getGuestSessionId().toString();
            }

            String value = ownerType + "|" + ownerIdentifier + "|" + expireAt;

            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, lockTimeout, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(success)) {
                successfullyLockedSeats.add(seatId);
                responses.add(buildLockResponse(req.getShowtimeId(), seatId, "LOCKED", lockTimeout));
            } else {
                log.warn("Seat {} of showtime {} already locked. Rolling back {} seats.",
                        seatId, req.getShowtimeId(), successfullyLockedSeats.size());

                deleteRedisLocks(req.getShowtimeId(), successfullyLockedSeats);

                throw new IllegalSeatLockException("Seat " + seatId + " is already locked by another user or session.");
            }
        }

        int updatedCount = showtimeSeatRepository.bulkUpdateSeatStatus(
                req.getShowtimeId(),
                seatIds,
                ShowtimeSeat.SeatStatus.LOCKED,
                LocalDateTime.now());

        if (req.getUserId() != null) {
            log.info("All {} seats locked (Redis+DB) for showtime {} by user {}. DB updated: {}",
                    seatIds.size(), req.getShowtimeId(), req.getUserId(), updatedCount);
        } else {
            log.info("All {} seats locked (Redis+DB) for showtime {} by guest [{} - {}]. DB updated: {}",
                    seatIds.size(),
                    req.getShowtimeId(),
                    req.getGuestSessionId(),
                    updatedCount);
        }

        for (UUID seatId : successfullyLockedSeats) {
            SeatLockResponse response = buildLockResponse(req.getShowtimeId(), seatId, "LOCKED", lockTimeout);
            webSocketHandler.broadcastToShowtime(req.getShowtimeId(), response);
        }

        return responses;
    }

    public void mapBookingIdToSeatLocks(BookingSeatMappedEvent event) {
        log.info("MAPPING: Received bookingId {} for showtime {}. Updating Redis locks...",
                event.bookingId(), event.showtimeId());

        String newBookingId = event.bookingId().toString();

        for (UUID seatId : event.seatIds()) {
            String lockKey = key(event.showtimeId(), seatId);

            String currentValue = redisTemplate.opsForValue().get(lockKey);

            // Kiểm tra: Lock còn tồn tại và chưa bị map (không bắt đầu bằng bookingId)
            if (currentValue != null && !currentValue.startsWith(newBookingId)) {
                // Giá trị mới: bookingId|userId|expireAt
                String newValue = newBookingId + "|" + currentValue;

                // Thực thi LUA Script để cập nhật giá trị và bảo toàn TTL
                Long result = redisTemplate.execute(
                        redisScript,
                        List.of(lockKey),
                        newValue);

                if (result == 1) {
                    Long currentTTL = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
                    if (currentTTL != null && currentTTL > 0) {
                        String mappingKey = BOOKING_MAPPING_KEY_PREFIX + event.showtimeId().toString() + ":"
                                + seatId.toString();

                        // Key mapping chỉ cần giữ bookingId
                        redisTemplate.opsForValue().set(mappingKey, newBookingId, currentTTL + 60, TimeUnit.SECONDS);
                        log.debug("MAPPING: Created dedicated mapping key {} with TTL {}s.", mappingKey,
                                currentTTL + 60);
                    }
                    log.debug("MAPPING: Successfully mapped booking {} to lock {}.", newBookingId, lockKey);
                } else {
                    log.warn("MAPPING: Lock {} expired or not found before mapping booking {}.", lockKey, newBookingId);
                }
            } else if (currentValue == null) {
                log.warn("MAPPING: Lock key {} already expired. Cannot map bookingId {}.", lockKey, newBookingId);
            }
        }
    }

    @Transactional
    public void confirmBookingSeats(BookingStatusUpdatedEvent event) {
        if (!"CONFIRMED".equals(event.newStatus())) {
            log.warn("RK confirmed received but event status is {}", event.newStatus());
            return;
        }

        int updated = showtimeSeatRepository.bulkUpdateSeatStatus(
                event.showtimeId(),
                event.seatIds(),
                ShowtimeSeat.SeatStatus.BOOKED,
                LocalDateTime.now());

        log.info("CONFIRMED: Bulk updated {} seats for booking {} to BOOKED.", updated, event.bookingId());

        // Xóa lock Redis
        deleteRedisLocks(event.showtimeId(), event.seatIds());
    }

    @Transactional
    public void releaseSeatsByBookingStatus(BookingStatusUpdatedEvent event) {
        String status = event.newStatus();
        if (!"CANCELLED".equals(status) && !"EXPIRED".equals(status) && !"REFUNDED".equals(status)) {
            log.warn("RK cancelled/expired/refunded received but event status is {}", status);
            return;
        }

        int updated = showtimeSeatRepository.bulkUpdateSeatStatus(
                event.showtimeId(),
                event.seatIds(),
                ShowtimeSeat.SeatStatus.AVAILABLE,
                LocalDateTime.now());

        log.info("RELEASED (Status: {}): Bulk updated {} seats for booking {}.", status, updated, event.bookingId());

        // Xóa lock Redis
        deleteRedisLocks(event.showtimeId(), event.seatIds());
    }

    @Transactional
    public List<SeatLockResponse> releaseSeats(UUID showtimeId, List<UUID> seatIds, UUID bookingId, String reason) {

        // 1. Xóa Redis
        deleteRedisLocks(showtimeId, seatIds);

        // 2. Cập nhật DB
        int updatedCount = showtimeSeatRepository.bulkUpdateSeatStatus(
                showtimeId,
                seatIds,
                ShowtimeSeat.SeatStatus.AVAILABLE,
                LocalDateTime.now());

        // 3. Gửi Event
        SeatUnlockedEvent event = new SeatUnlockedEvent(
                bookingId,
                showtimeId,
                seatIds,
                reason);
        showtimeProducer.sendSeatUnlockedEvent(event);

        log.info("Released {} seats (Redis+DB) for showtime {} (Reason: {}). DB updated: {}",
                seatIds.size(), showtimeId, reason, updatedCount);

        // 4. Trả về phản hồi
        return seatIds.stream()
                .map(seatId -> buildLockResponse(showtimeId, seatId, "AVAILABLE", 0))
                .toList();
    }

    private static final String BOOKING_MAPPING_KEY_PREFIX = "booking_seat_map:"; // Giữ nguyên

    @Transactional
    public void handleExpiredLock(UUID showtimeId, UUID seatId) {

        int updatedCount = showtimeSeatRepository.bulkUpdateSeatStatus(
                showtimeId,
                Collections.singletonList(seatId), // Chỉ là 1 ghế
                ShowtimeSeat.SeatStatus.AVAILABLE,
                LocalDateTime.now());

        log.info("EXPIRED: Seat {} of showtime {} status reset to AVAILABLE. DB updated: {}",
                seatId, showtimeId, updatedCount);

        // 2. Xây dựng key mapping để lấy Booking ID
        String mappingKey = BOOKING_MAPPING_KEY_PREFIX + showtimeId.toString() + ":" + seatId.toString();

        // 3. Lấy Booking ID từ Redis (Key này đã được đảm bảo tồn tại lâu hơn lockKey
        // chính)
        String bookingIdStr = redisTemplate.opsForValue().get(mappingKey);

        if (bookingIdStr != null) {
            try {
                UUID bookingId = UUID.fromString(bookingIdStr);

                // 4. Gửi sự kiện SeatUnlockedEvent cho Booking Service
                log.warn("TTL EXPIRED: Found mapping for Booking {}. Sending SeatUnlockedEvent.",
                        bookingId);

                // Tạo Event với thông tin của bạn
                SeatUnlockedEvent event = new SeatUnlockedEvent(
                        bookingId,
                        showtimeId,
                        Collections.singletonList(seatId),
                        "SEAT_LOCK_EXPIRED");

                showtimeProducer.sendSeatUnlockedEvent(event);

                // 5. Xóa key mapping để dọn dẹp Redis

                redisTemplate.delete(mappingKey);

            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format stored in Redis for key: {}", mappingKey, e);
            }
        } else {
            log.warn("Key mapping not found for expired seat lock: {} (Lock was likely handled by another event).",
                    mappingKey);
        }
    }

    public SeatLockResponse seatStatus(UUID showtimeId, UUID seatId) {
        String key = key(showtimeId, seatId);
        boolean locked = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        long ttl = locked ? ttl(showtimeId, seatId) : 0;

        return SeatLockResponse.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status(locked ? "LOCKED" : "AVAILABLE")
                .ttl(ttl)
                .build();
    }

    private void deleteRedisLocks(UUID showtimeId, List<UUID> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> key(showtimeId, seatId))
                .toList();

        Long deletedCount = redisTemplate.delete(keys);
        log.debug("Deleted {} Redis lock keys for showtime {}.", deletedCount, showtimeId);
    }

    private long ttl(UUID showtimeId, UUID seatId) {
        Long ttl = redisTemplate.getExpire(key(showtimeId, seatId), TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private String key(UUID showtimeId, UUID seatId) {
        return "seat:" + showtimeId + ":" + seatId;
    }

    private SeatLockResponse buildLockResponse(UUID showtimeId, UUID seatId, String status, long ttl) {
        return SeatLockResponse.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status(status)
                .ttl(ttl)
                .build();
    }
}