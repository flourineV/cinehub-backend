package com.cinehub.showtime.repository;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.entity.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {

    // Lấy tất cả ghế của 1 suất chiếu
    List<ShowtimeSeat> findByShowtime_Id(UUID showtimeId);

    // Lấy theo showtime + seat cụ thể
    Optional<ShowtimeSeat> findByShowtime_IdAndSeat_Id(UUID showtimeId, UUID seatId);

    @Query("""
                SELECT new com.cinehub.showtime.dto.response.ShowtimeSeatResponse(
                    seat.id,
                    seat.seatNumber,
                    seat.type,
                    s.status
                )
                FROM ShowtimeSeat s
                JOIN s.seat seat
                WHERE s.showtime.id = :showtimeId
                ORDER BY seat.rowLabel, seat.seatNumber
            """)
    List<ShowtimeSeatResponse> findSeatResponsesByShowtimeId(@Param("showtimeId") UUID showtimeId);

    /**
     * Cập nhật trạng thái 1 ghế cụ thể trong suất chiếu.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
                UPDATE ShowtimeSeat s
                SET s.status = :status, s.updatedAt = :now
                WHERE s.showtime.id = :showtimeId
                AND s.seat.id = :seatId
            """)
    int updateSingleSeatStatus(@Param("showtimeId") UUID showtimeId,
            @Param("seatId") UUID seatId,
            @Param("status") ShowtimeSeat.SeatStatus status,
            @Param("now") LocalDateTime now);

    /**
     * Cập nhật trạng thái hàng loạt cho các ghế cụ thể trong một suất chiếu.
     * Sử dụng s.seat.id (ID ghế rạp) làm điều kiện.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
                UPDATE ShowtimeSeat s
                SET s.status = :status, s.updatedAt = :now
                WHERE s.showtime.id = :showtimeId
                AND s.seat.id IN :seatIds
            """)
    int bulkUpdateSeatStatus(@Param("showtimeId") UUID showtimeId,
            @Param("seatIds") List<UUID> seatIds,
            @Param("status") ShowtimeSeat.SeatStatus status,
            @Param("now") LocalDateTime now);

    /**
     * Count booked seats for a showtime
     */
    @Query("""
                SELECT COUNT(s)
                FROM ShowtimeSeat s
                WHERE s.showtime.id = :showtimeId
                AND s.status IN ('BOOKED', 'RESERVED')
            """)
    long countBookedSeatsByShowtimeId(@Param("showtimeId") UUID showtimeId);

}