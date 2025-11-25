package com.cinehub.booking.repository;

import com.cinehub.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.cinehub.booking.entity.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.userId = :userId
            AND (b.status = 'CANCELLED' OR b.status = 'REFUNDED')
            AND b.updatedAt >= :startOfMonth
            """)
    long countCancelledBookingsInMonth(UUID userId, java.time.LocalDateTime startOfMonth);

    List<Booking> findByShowtimeIdAndStatus(UUID showtimeId, BookingStatus status);

}