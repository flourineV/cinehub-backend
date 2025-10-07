package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.entity.ShowtimeSeat.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {

    // Lấy tất cả ghế của 1 suất chiếu
    List<ShowtimeSeat> findByShowtime_Id(UUID showtimeId);

    // Lấy theo showtime + seat cụ thể (để update nhanh)
    Optional<ShowtimeSeat> findByShowtime_IdAndSeat_Id(UUID showtimeId, UUID seatId);

    // Đếm số ghế theo trạng thái (cho dashboard thống kê)
    long countByShowtime_IdAndStatus(UUID showtimeId, SeatStatus status);
}