package com.cinehub.showtime.service;

import com.cinehub.showtime.repository.SeatRepository;
import com.cinehub.showtime.dto.request.SeatSelectionDetail;
import com.cinehub.showtime.dto.response.SeatLockResponse;
import com.cinehub.showtime.events.SeatLockedEvent;
import com.cinehub.showtime.events.SeatUnlockedEvent;
import com.cinehub.showtime.producer.ShowtimeProducer;
import com.cinehub.showtime.entity.Seat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;
    private final ShowtimeProducer showtimeProducer;
    // private final SeatRepository seatRepository; // 💡 Bỏ nếu không cần, hoặc giữ
    // lại cho các nghiệp vụ khác

    @Value("${lock.timeout:10000}")
    private int lockTimeout;

    // SỬA: Thay đổi tham số từ List<UUID> sang List<SeatSelectionDetail>
    public List<SeatLockResponse> lockSeats(UUID showtimeId, List<SeatSelectionDetail> selectedSeats, UUID userId) {

        List<SeatLockResponse> responses = new java.util.ArrayList<>();
        List<UUID> successfullyLockedSeats = new java.util.ArrayList<>();

        // Trích xuất list ID ghế để dễ dàng sử dụng cho rollback/log
        List<UUID> seatIds = selectedSeats.stream().map(SeatSelectionDetail::getSeatId).toList();

        // 1. Khóa từng ghế với cơ chế All or Nothing
        for (SeatSelectionDetail seatDetail : selectedSeats) {
            UUID seatId = seatDetail.getSeatId();
            String key = key(showtimeId, seatId);
            long expireAt = System.currentTimeMillis() + lockTimeout * 1000L;
            String value = userId + "|" + expireAt;

            // Sử dụng SETNX
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, lockTimeout, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(success)) {
                successfullyLockedSeats.add(seatId);
                responses.add(buildLockResponse(showtimeId, seatId, "LOCKED", lockTimeout));
            } else {
                // 2. Nếu một ghế bị khóa -> Rollback (giải phóng) tất cả các ghế đã khóa thành
                // công
                log.warn("⚠️ Seat {} of showtime {} already locked. Rolling back all {} locked seats.",
                        seatId, showtimeId, successfullyLockedSeats.size());

                // ROLLBACK
                releaseSeats(showtimeId, successfullyLockedSeats);

                // Trả về phản hồi lỗi cho toàn bộ request
                return buildFailureResponse(showtimeId, seatIds, "CONFLICT", remainingTtl(key));
            }
        }

        // 3. Nếu khóa thành công TẤT CẢ -> Gửi event
        // KHÔNG CẦN truy vấn DB vì thông tin đã có trong selectedSeats

        // SỬA: Gửi Event với List<SeatSelectionDetail>
        SeatLockedEvent event = new SeatLockedEvent(
                userId,
                showtimeId,
                selectedSeats, // Truyền toàn bộ chi tiết ghế
                lockTimeout);
        showtimeProducer.sendSeatLockedEvent(event);

        log.info("🎟️ All {} seats locked for showtime {} by user {}",
                seatIds.size(), showtimeId, userId);

        return responses; // Trả về danh sách phản hồi thành công
    }

    /**
     * Tính TTL còn lại của ghế đang bị lock trong Redis
     */
    private long remainingTtl(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || !value.contains("|"))
            return 0;
        long expireAt = Long.parseLong(value.split("\\|")[1]);
        long remaining = (expireAt - System.currentTimeMillis()) / 1000L;
        return Math.max(remaining, 0);
    }

    /**
     * Giải phóng nhiều ghế (khi user huỷ, timeout, hoặc rollback)
     */
    // Giữ nguyên logic, chỉ cần List<UUID>
    public List<SeatLockResponse> releaseSeats(UUID showtimeId, List<UUID> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> key(showtimeId, seatId))
                .toList();

        redisTemplate.delete(keys); // Xóa hàng loạt keys

        // Gửi event giải phóng
        SeatUnlockedEvent event = new SeatUnlockedEvent(
                showtimeId,
                seatIds,
                "cancelled");
        showtimeProducer.sendSeatUnlockedEvent(event);

        log.info("🔓 Released {} seats for showtime {}", seatIds.size(), showtimeId);

        // Bổ sung logic trả về List các phản hồi thành công
        return seatIds.stream()
                .map(seatId -> buildLockResponse(showtimeId, seatId, "AVAILABLE", 0))
                .toList();
    }

    /**
     * Kiểm tra trạng thái ghế (LOCKED / AVAILABLE)
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

    // ===== Helper =====

    private long ttl(UUID showtimeId, UUID seatId) {
        Long ttl = redisTemplate.getExpire(key(showtimeId, seatId), TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private String key(UUID showtimeId, UUID seatId) {
        return "seat:" + showtimeId + ":" + seatId;
    }

    /**
     * Hàm trợ giúp để xây dựng phản hồi khóa thành công/thất bại cho một ghế.
     */
    private SeatLockResponse buildLockResponse(UUID showtimeId, UUID seatId, String status, long ttl) {
        return SeatLockResponse.builder()
                .showtimeId(showtimeId)
                .seatId(seatId)
                .status(status) // Ví dụ: "LOCKED" hoặc "AVAILABLE"
                .ttl(ttl)
                .build();
    }

    /**
     * Hàm trợ giúp để xây dựng danh sách phản hồi thất bại cho toàn bộ request.
     */
    private List<SeatLockResponse> buildFailureResponse(UUID showtimeId, List<UUID> seatIds, String status, long ttl) {
        List<SeatLockResponse> responses = new java.util.ArrayList<>();

        // Trả về phản hồi lỗi cho TOÀN BỘ các ghế trong list.
        for (UUID seatId : seatIds) {
            responses.add(SeatLockResponse.builder()
                    .showtimeId(showtimeId)
                    .seatId(seatId)
                    .status(status) // Ví dụ: "CONFLICT"
                    .ttl(ttl) // Thời gian TTL còn lại của ghế đã bị khóa (nếu có)
                    .build());
        }
        return responses;
    }
}