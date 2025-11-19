package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.request.SeatRequest;
import com.cinehub.showtime.dto.response.SeatResponse;
import com.cinehub.showtime.entity.Seat;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.repository.SeatRepository;
import com.cinehub.showtime.repository.RoomRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

        private final SeatRepository seatRepository;
        private final RoomRepository roomRepository;

        public SeatResponse createSeats(SeatRequest request) {
                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Room with ID " + request.getRoomId() + " not found"));

                Seat seat = Seat.builder()
                                .seatNumber(request.getSeatNumber())
                                .rowLabel(request.getRowLabel())
                                .columnIndex(request.getColumnIndex())
                                .type(request.getType())
                                .room(room)
                                .build();
                Seat savedSeat = seatRepository.save(seat);
                return mapToSeatResponse(savedSeat);
        }

        public List<SeatResponse> createSeats(List<SeatRequest> requests) {
                if (requests == null || requests.isEmpty()) {
                        return List.of(); // Trả về danh sách rỗng nếu không có request
                }

                // Lấy roomId từ request đầu tiên (Giả định tất cả ghế thuộc cùng một Room)
                UUID roomId = requests.get(0).getRoomId();

                // Chỉ tìm Room một lần
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(
                                                () -> new EntityNotFoundException("Room with ID " + roomId
                                                                + " not found for bulk creation"));

                // Chuyển đổi List<SeatRequest> sang List<Seat> (Entity)
                List<Seat> seatsToSave = requests.stream()
                                .map(request -> Seat.builder()
                                                .seatNumber(request.getSeatNumber())
                                                .rowLabel(request.getRowLabel())
                                                .columnIndex(request.getColumnIndex())
                                                .type(request.getType())
                                                .room(room) // Gán đối tượng Room đã tìm được
                                                .build())
                                .collect(Collectors.toList()); // Lưu tất cả entity bằng một lệnh (tối ưu database)
                List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);

                // Chuyển đổi List<Seat> (Entity) sang List<SeatResponse>
                return savedSeats.stream()
                                .map(this::mapToSeatResponse)
                                .collect(Collectors.toList());
        }

        public SeatResponse getSeatById(UUID id) {
                Seat seat = seatRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Seat with ID " + id + " not found"));
                return mapToSeatResponse(seat);
        }

        public List<SeatResponse> getAllSeats() {
                return seatRepository.findAll().stream()
                                .map(this::mapToSeatResponse)
                                .collect(Collectors.toList());
        }

        public List<SeatResponse> getSeatsByRoomId(UUID roomId) {
                return seatRepository.findByRoomId(roomId).stream()
                                .map(this::mapToSeatResponse)
                                .collect(Collectors.toList());
        }

        public SeatResponse updateSeat(UUID id, SeatRequest request) {
                Seat seat = seatRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Seat with ID " + id + " not found"));
                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Room with ID " + request.getRoomId() + " not found"));

                seat.setSeatNumber(request.getSeatNumber());
                seat.setRowLabel(request.getRowLabel());
                seat.setColumnIndex(request.getColumnIndex());
                seat.setType(request.getType());
                seat.setRoom(room);

                Seat updatedSeat = seatRepository.save(seat);
                return mapToSeatResponse(updatedSeat);
        }

        public void deleteSeat(UUID id) {
                if (!seatRepository.existsById(id)) {
                        throw new EntityNotFoundException("Seat with ID " + id + " not found for deletion");
                }
                seatRepository.deleteById(id);
        }

        // --- Helper function: Mapping từ Entity sang Response DTO ---
        private SeatResponse mapToSeatResponse(Seat seat) {
                return SeatResponse.builder()
                                .id(seat.getId())
                                .seatNumber(seat.getSeatNumber())
                                .rowLabel(seat.getRowLabel())
                                .columnIndex(seat.getColumnIndex())
                                .type(seat.getType())
                                .roomName(seat.getRoom().getName())
                                .build();
        }
}