package com.cinehub.booking.repository;

import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, BookingRepositoryCustom {

        List<Booking> findByUserId(UUID userId);

        @Query("""
                        SELECT COUNT(b) FROM Booking b
                        WHERE b.userId = :userId
                        AND (b.status = 'CANCELLED' OR b.status = 'REFUNDED')
                        AND b.updatedAt >= :startOfMonth
                        """)
        long countCancelledBookingsInMonth(UUID userId, LocalDateTime startOfMonth);

        List<Booking> findByShowtimeIdAndStatus(UUID showtimeId, BookingStatus status);

}