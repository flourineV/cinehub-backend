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

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.userId = :userId
            AND (b.status = 'CANCELLED' OR b.status = 'REFUNDED')
            AND b.updatedAt >= :startOfMonth
            """)
    long countCancelledBookingsInMonth(UUID userId, LocalDateTime startOfMonth);

    List<Booking> findByShowtimeIdAndStatus(UUID showtimeId, BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE (:userId IS NULL OR b.userId = :userId)
            AND (:bookingCode IS NULL OR LOWER(b.BookingCode) LIKE LOWER(CONCAT('%', :bookingCode, '%')))
            AND (:status IS NULL OR b.status = :status)
            AND (:paymentMethod IS NULL OR b.paymentMethod = :paymentMethod)
            AND (:guestName IS NULL OR LOWER(b.guestName) LIKE LOWER(CONCAT('%', :guestName, '%')))
            AND (:guestEmail IS NULL OR LOWER(b.guestEmail) LIKE LOWER(CONCAT('%', :guestEmail, '%')))
            AND (:fromDate IS NULL OR b.createdAt >= :fromDate)
            AND (:toDate IS NULL OR b.createdAt <= :toDate)
            AND (:minPrice IS NULL OR b.finalPrice >= :minPrice)
            AND (:maxPrice IS NULL OR b.finalPrice <= :maxPrice)
            """)
    Page<Booking> searchBookingsWithFilters(
            @Param("userId") UUID userId,
            @Param("showtimeId") UUID showtimeId,
            @Param("bookingCode") String bookingCode,
            @Param("status") BookingStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("guestName") String guestName,
            @Param("guestEmail") String guestEmail,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}