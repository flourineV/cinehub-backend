package com.cinehub.showtime.controller;

import com.cinehub.showtime.dto.request.RoomRequest;
import com.cinehub.showtime.dto.response.RoomResponse;
import com.cinehub.showtime.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable UUID id) {
        RoomResponse response = roomService.getRoomById(id);
        // Trả về Status Code 200 OK
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        List<RoomResponse> responseList = roomService.getAllRooms();
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable UUID id,
            @RequestBody RoomRequest request) {

        RoomResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    // --- (Optional) GET: Lấy danh sách Room theo Theater ID ---
    // Đây là một endpoint thường gặp để lấy tất cả phòng của một rạp cụ thể
    // Endpoint: GET /api/rooms/by-theater/{theaterId}
    /*
     * @GetMapping("/by-theater/{theaterId}")
     * public ResponseEntity<List<RoomResponse>> getRoomsByTheaterId(@PathVariable
     * String theaterId) {
     * // Giả sử bạn thêm phương thức này vào RoomService:
     * // List<RoomResponse> responseList =
     * roomService.getRoomsByTheaterId(theaterId);
     * // return ResponseEntity.ok(responseList);
     * return ResponseEntity.ok(roomService.getRoomsByTheaterId(theaterId));
     * }
     */
}