package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByRoomId(UUID roomId);

    int countByRoomId(UUID roomId);
}
