package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.response.ShowtimeSeatResponse;
import com.cinehub.showtime.dto.response.ShowtimeSeatsLayoutResponse;
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

        public ShowtimeSeatsLayoutResponse getSeatsByShowtime(UUID showtimeId) {
                List<ShowtimeSeatResponse> seats = showtimeSeatRepository.findSeatResponsesByShowtimeId(showtimeId);

                // Calculate layout metadata from seatNumber (format: A1, B5, etc.)
                int totalSeats = seats.size();
                int maxRow = seats.stream()
                                .map(ShowtimeSeatResponse::getSeatNumber)
                                .filter(sn -> sn != null && !sn.isEmpty())
                                .map(sn -> sn.charAt(0)) // Get row letter
                                .mapToInt(c -> c - 'A' + 1)
                                .max()
                                .orElse(0);
                int maxColumn = seats.stream()
                                .map(ShowtimeSeatResponse::getSeatNumber)
                                .filter(sn -> sn != null && sn.length() > 1)
                                .map(sn -> sn.substring(1)) // Get column number
                                .mapToInt(Integer::parseInt)
                                .max()
                                .orElse(0);

                return ShowtimeSeatsLayoutResponse.builder()
                                .totalSeats(totalSeats)
                                .totalRows(maxRow)
                                .totalColumns(maxColumn)
                                .seats(seats)
                                .build();
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

        @Transactional
        public int batchInitializeSeats(List<UUID> showtimeIds) {
                int count = 0;
                for (UUID showtimeId : showtimeIds) {
                        try {
                                initializeSeatsForShowtime(showtimeId);
                                count++;
                        } catch (Exception e) {
                                // Log error nhưng tiếp tục với các showtime khác
                                System.err.println("Failed to initialize seats for showtime " + showtimeId + ": "
                                                + e.getMessage());
                        }
                }
                return count;
        }

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
                                .type(seat.getSeat().getType())
                                .status(seat.getStatus())
                                .build();
        }
}
