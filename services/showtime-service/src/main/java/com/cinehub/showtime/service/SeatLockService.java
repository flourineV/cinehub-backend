package com.cinehub.showtime.service;

import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import com.cinehub.showtime.dto.request.SeatSelectionDetail;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import com.cinehub.showtime.events.BookingStatusUpdatedEvent; // ✅ Event mới
import com.cinehub.showtime.events.BookingSeatMappedEvent; // ✅ Event mới
import com.cinehub.showtime.events.SeatLockedEvent;
import com.cinehub.showtime.events.SeatUnlockedEvent;
import com.cinehub.showtime.exception.IllegalSeatLockException;
import com.cinehub.showtime.producer.ShowtimeProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript; // ✅ Import Script
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private final ShowtimeProducer showtimeProducer;
    private final ShowtimeSeatRepository showtimeSeatRepository; // Repository DB

    @Value("${lock.timeout:20}")
    private int lockTimeout;

    // =======================================================================================
    // REDIS LUA SCRIPT CHO ÁNH XẠ BOOKING ID
    // =======================================================================================
    private static final String UPDATE_LOCK_WITH_TTL_SCRIPT = """
            local ttl = redis.call('TTL', KEYS[1])
            if ttl > 0 then
                redis.call('SET', KEYS[1], ARGV[1], 'EX', ttl)
                return 1
            else
                return 0
            end
            """;

    // Khai báo script để Spring Data Redis có thể sử dụng
    private final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UPDATE_LOCK_WITH_TTL_SCRIPT,
            Long.class);

    // =======================================================================================
    // 1. LOGIC KHÓA GHẾ (API User)
    // =======================================================================================
    @Transactional
    public List<SeatLockResponse> lockSeats(UUID showtimeId, List<SeatSelectionDetail> selectedSeats, UUID userId) {

        List<SeatLockResponse> responses = new java.util.ArrayList<>();
        List<UUID> successfullyLockedSeats = new java.util.ArrayList<>();
        List<UUID> seatIds = selectedSeats.stream().map(SeatSelectionDetail::getSeatId).toList();

        for (SeatSelectionDetail seatDetail : selectedSeats) {
            UUID seatId = seatDetail.getSeatId();
            String key = key(showtimeId, seatId);
            long expireAt = System.currentTimeMillis() + lockTimeout * 1000L;
            // Giá trị ban đầu: userId|expireAt
            String value = userId + "|" + expireAt;

            // Sử dụng SETNX (Set if Not Exists)
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, lockTimeout, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(success)) {
                successfullyLockedSeats.add(seatId);
                responses.add(buildLockResponse(showtimeId, seatId, "LOCKED", lockTimeout));
            } else {
                log.warn("⚠️ Seat {} of showtime {} already locked. Rolling back {} seats.",
                        seatId, showtimeId, successfullyLockedSeats.size());

                // ❌ SỬA: CHỈ DÙNG deleteRedisLocks để xóa locks đã tạo thành công
                deleteRedisLocks(showtimeId, successfullyLockedSeats);

                // Ném ngoại lệ để Spring Transaction rollback DB
                throw new IllegalSeatLockException("Seat " + seatId + " is already locked by another user or session.");
            }
        }

        // 3. Nếu khóa thành công TẤT CẢ (Redis OK) -> Cập nhật DB
        int updatedCount = showtimeSeatRepository.bulkUpdateSeatStatus(
                showtimeId,
                seatIds,
                ShowtimeSeat.SeatStatus.LOCKED, // Trạng thái mới trong DB
                LocalDateTime.now());

        // 4. Gửi Event
        SeatLockedEvent event = new SeatLockedEvent(
                userId,
                showtimeId,
                selectedSeats,
                lockTimeout);
        showtimeProducer.sendSeatLockedEvent(event);

        log.info("🎟️ All {} seats locked (Redis+DB) for showtime {} by user {}. DB updated: {}",
                seatIds.size(), showtimeId, userId, updatedCount);

        return responses;
    }

    // =======================================================================================
    // 2. LOGIC XỬ LÝ EVENT TỪ BOOKING SERVICE (Hàm chuyển từ
    // SeatStatusUpdateService)
    // =======================================================================================

    /**
     * ✅ Xử lý Event BOOKING_SEAT_MAPPED: Ánh xạ bookingId vào giá trị Redis Lock.
     */
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
                    log.debug("MAPPING: Successfully mapped booking {} to lock {}.", newBookingId, lockKey);
                } else {
                    log.warn("MAPPING: Lock {} expired or not found before mapping booking {}.", lockKey, newBookingId);
                }
            } else if (currentValue == null) {
                log.warn("MAPPING: Lock key {} already expired. Cannot map bookingId {}.", lockKey, newBookingId);
            }
        }
    }

    /**
     * ✅ Xử lý Event BOOKING_CONFIRMED: Chuyển ghế từ LOCKED -> BOOKED và xóa lock
     * Redis.
     */
    @Transactional
    public void confirmBookingSeats(BookingStatusUpdatedEvent event) {
        if (!"CONFIRMED".equals(event.status())) {
            log.warn("RK confirmed received but event status is {}", event.status());
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

    /**
     * ✅ Xử lý Event CANCELLED/EXPIRED: Chuyển ghế từ LOCKED -> AVAILABLE và xóa
     * lock Redis.
     */
    @Transactional
    public void releaseSeatsByBookingStatus(BookingStatusUpdatedEvent event) {
        String status = event.status();
        if (!"CANCELLED".equals(status) && !"EXPIRED".equals(status)) {
            log.warn("RK cancelled/expired received but event status is {}", status);
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

    /**
     * ✅ Xử lý Event SEAT_RELEASE_REQUEST: Lệnh mở khóa khẩn cấp.
     */
    @Transactional
    public void releaseSeatsByCommand(SeatUnlockedEvent event) {
        int updated = showtimeSeatRepository.bulkUpdateSeatStatus(
                event.showtimeId(),
                event.seatIds(),
                ShowtimeSeat.SeatStatus.AVAILABLE,
                LocalDateTime.now());

        log.info("RELEASED (Command: {}): Bulk updated {} seats for booking {}.", event.reason(), updated,
                event.bookingId());

        // Xóa lock Redis
        deleteRedisLocks(event.showtimeId(), event.seatIds());
    }

    // =======================================================================================
    // 3. LOGIC GIẢI PHÓNG GHẾ (API/Hàm chung)
    // =======================================================================================

    /**
     * Giải phóng nhiều ghế (khi user huỷ) - Public API
     */
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

        log.info("🔓 Released {} seats (Redis+DB) for showtime {} (Reason: {}). DB updated: {}",
                seatIds.size(), showtimeId, reason, updatedCount);

        // 4. Trả về phản hồi
        return seatIds.stream()
                .map(seatId -> buildLockResponse(showtimeId, seatId, "AVAILABLE", 0))
                .toList();
    }

    /**
     * Kiểm tra trạng thái ghế (LOCKED / AVAILABLE) - Public API
     */
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

    // =======================================================================================
    // 4. HÀM HỖ TRỢ (HELPER METHODS)
    // =======================================================================================

    /**
     * Hàm nội bộ CHỈ XÓA Redis locks.
     */
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