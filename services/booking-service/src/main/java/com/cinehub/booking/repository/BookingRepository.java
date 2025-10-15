package com.cinehub.booking.repository;

import com.cinehub.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Lấy danh sách Booking của người dùng
    List<Booking> findByUserId(UUID userId);

    // Tùy chọn: Lấy Booking bằng ID và đảm bảo khóa nó cho giao dịch (dùng cho
    // logic phức tạp)
    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Optional<Booking> findById(UUID id);
}