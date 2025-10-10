package com.cinehub.showtime.repository;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.entity.ShowtimeSeat.SeatStatus;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {

    // Lấy tất cả ghế của 1 suất chiếu
    List<ShowtimeSeat> findByShowtime_Id(UUID showtimeId);

    // Lấy theo showtime + seat cụ thể (để update nhanh)
    Optional<ShowtimeSeat> findByShowtime_IdAndSeat_Id(UUID showtimeId, UUID seatId);

    @Query("""
                SELECT new com.cinehub.showtime.dto.response.ShowtimeSeatResponse(
                    seat.id,
                    seat.seatNumber,
                    s.status
                )
                FROM ShowtimeSeat s
                JOIN s.seat seat
                WHERE s.showtime.id = :showtimeId
                ORDER BY seat.rowLabel, seat.seatNumber
            """)
    List<ShowtimeSeatResponse> findSeatResponsesByShowtimeId(@Param("showtimeId") UUID showtimeId);

    @Query("SELECT s FROM ShowtimeSeat s WHERE s.showtime.id = :showtimeId AND s.id IN :seatIds")
    List<ShowtimeSeat> findSeatsForLock(@Param("showtimeId") UUID showtimeId, @Param("seatIds") List<UUID> seatIds);

    @Modifying
    @Transactional
    @Query("UPDATE ShowtimeSeat s SET s.status = :status WHERE s.id IN :seatIds")
    void updateSeatStatus(@Param("seatIds") List<UUID> seatIds, @Param("status") SeatStatus status);

    @Modifying(clearAutomatically = true)
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

}