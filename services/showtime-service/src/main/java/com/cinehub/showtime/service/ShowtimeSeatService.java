package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.dto.request.UpdateSeatStatusRequest;
import com.cinehub.showtime.entity.Seat;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.entity.ShowtimeSeat.SeatStatus;
import com.cinehub.showtime.repository.SeatRepository;
import com.cinehub.showtime.repository.ShowtimeRepository;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowtimeSeatService {

        private final ShowtimeSeatRepository showtimeSeatRepository;
        private final ShowtimeRepository showtimeRepository;
        private final SeatRepository seatRepository;

        public List<ShowtimeSeatResponse> getSeatsByShowtime(UUID showtimeId) {
                return showtimeSeatRepository.findSeatResponsesByShowtimeId(showtimeId);
        }

        @Transactional
        public ShowtimeSeatResponse updateSeatStatus(UpdateSeatStatusRequest request) {
                ShowtimeSeat seat = showtimeSeatRepository
                                .findByShowtime_IdAndSeat_Id(request.getShowtimeId(), request.getSeatId())
                                .orElseThrow(() -> new RuntimeException("Seat not found for this showtime"));

                seat.setStatus(request.getStatus());
                seat.setUpdatedAt(LocalDateTime.now());

                ShowtimeSeat saved = showtimeSeatRepository.save(seat);
                return toResponse(saved);
        }

        /**
         * Hàm khởi tạo trạng thái ghế khi tạo showtime mới
         * (tất cả ghế AVAILABLE)
         */
        @Transactional
        public void initializeSeatsForShowtime(UUID showtimeId) {
                Showtime showtime = showtimeRepository.findById(showtimeId)
                                .orElseThrow(() -> new RuntimeException("Showtime not found"));

                UUID roomId = showtime.getRoom().getId();
                List<Seat> seats = seatRepository.findByRoomId(roomId);

                List<ShowtimeSeat> showtimeSeats = seats.stream()
                                .map(s -> ShowtimeSeat.builder()
                                                .showtime(showtime)
                                                .seat(s)
                                                .status(SeatStatus.AVAILABLE)
                                                .updatedAt(LocalDateTime.now())
                                                .build())
                                .toList();

                showtimeSeatRepository.saveAll(showtimeSeats);
        }

        private ShowtimeSeatResponse toResponse(ShowtimeSeat seat) {
                return ShowtimeSeatResponse.builder()
                                .seatId(seat.getSeat().getId())
                                .seatNumber(seat.getSeat().getSeatNumber())
                                .status(seat.getStatus())
                                .build();
        }
}
