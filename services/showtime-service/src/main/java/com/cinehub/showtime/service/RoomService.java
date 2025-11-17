package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.request.RoomRequest;
import com.cinehub.showtime.dto.response.RoomResponse;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.repository.RoomRepository;
import com.cinehub.showtime.repository.TheaterRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final TheaterRepository theaterRepository; // Cần để tìm Theater Entity

    public RoomResponse createRoom(RoomRequest request) {
        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Theater with ID " + request.getTheaterId() + " not found"));

        Room room = Room.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .seatCount(request.getSeatCount())
                .theater(theater)
                .build();

        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    public RoomResponse getRoomById(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room with ID " + id + " not found"));

        return mapToRoomResponse(room);
    }

    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getRoomsByTheaterId(UUID theaterId) {
        return roomRepository.findByTheaterId(theaterId).stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse updateRoom(UUID id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room with ID " + id + " not found"));

        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Theater with ID " + request.getTheaterId() + " not found"));

        room.setName(request.getName());
        room.setSeatCount(request.getSeatCount());
        room.setTheater(theater);
        Room updatedRoom = roomRepository.save(room);
        return mapToRoomResponse(updatedRoom);
    }

    public void deleteRoom(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Room with ID " + id + " not found for deletion");
        }
        roomRepository.deleteById(id);
    }

    // --- Helper function: Mapping từ Entity sang Response DTO ---
    private RoomResponse mapToRoomResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .seatCount(room.getSeatCount())
                .theaterName(room.getTheater().getName())
                .build();
    }
}