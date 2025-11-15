package com.cinehub.showtime.service;

import com.cinehub.showtime.dto.request.ShowtimeRequest;
import com.cinehub.showtime.dto.response.ShowtimeResponse;
import com.cinehub.showtime.dto.response.ShowtimesByMovieResponse;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.repository.ShowtimeRepository;
import com.cinehub.showtime.repository.TheaterRepository;
import com.cinehub.showtime.repository.RoomRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final TheaterRepository theaterRepository;
    private final RoomRepository roomRepository;

    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Theater with ID " + request.getTheaterId() + " not found"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room with ID " + request.getRoomId() + " not found"));

        // 2. KIỂM TRA TRÙNG LỊCH (Gọi hàm helper)
        checkOverlap(request.getRoomId(), request.getStartTime(), request.getEndTime(), null);

        Showtime showtime = Showtime.builder()
                .id(UUID.randomUUID())
                .movieId(request.getMovieId())
                .theater(theater)
                .room(room)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Showtime savedShowtime = showtimeRepository.save(showtime);
        return mapToShowtimeResponse(savedShowtime);
    }

    public ShowtimeResponse getShowtimeById(UUID id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime with ID " + id + " not found"));

        return mapToShowtimeResponse(showtime);
    }

    public List<ShowtimeResponse> getAllShowtimes() {
        return showtimeRepository.findAll().stream()
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());
    }

    public List<ShowtimeResponse> getShowtimesByTheaterAndDate(UUID theaterId, LocalDateTime start,
            LocalDateTime end) {
        return showtimeRepository.findByTheaterIdAndStartTimeBetween(theaterId, start, end).stream()
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());
    }

    public List<ShowtimeResponse> getShowtimesByMovie(UUID movieId) {
        return showtimeRepository.findByMovieId(movieId).stream()
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());
    }

    public ShowtimesByMovieResponse getShowtimesByMovieGrouped(UUID movieId) {
        List<Showtime> showtimes = showtimeRepository.findByMovieId(movieId);

        // Map to response
        List<ShowtimeResponse> showtimeResponses = showtimes.stream()
                .map(this::mapToShowtimeResponse)
                .collect(Collectors.toList());

        // Group by date
        Map<LocalDate, List<ShowtimeResponse>> showtimesByDate = showtimeResponses.stream()
                .collect(Collectors.groupingBy(
                        st -> st.getStartTime().toLocalDate()));

        // Extract available dates and sort
        List<LocalDate> availableDates = showtimesByDate.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        return ShowtimesByMovieResponse.builder()
                .availableDates(availableDates)
                .showtimesByDate(showtimesByDate)
                .build();
    }

    public ShowtimeResponse updateShowtime(UUID id, ShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime with ID " + id + " not found"));

        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Theater with ID " + request.getTheaterId() + " not found"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room with ID " + request.getRoomId() + " not found"));

        // KIỂM TRA TRÙNG LỊCH (Gọi hàm helper, truyền id suất chiếu hiện tại để loại
        // trừ)
        checkOverlap(request.getRoomId(), request.getStartTime(), request.getEndTime(), id);

        showtime.setMovieId(request.getMovieId());
        showtime.setTheater(theater);
        showtime.setRoom(room);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());

        Showtime updatedShowtime = showtimeRepository.save(showtime);
        return mapToShowtimeResponse(updatedShowtime);
    }

    public void deleteShowtime(UUID id) {
        if (!showtimeRepository.existsById(id)) {
            throw new EntityNotFoundException("Showtime with ID " + id + " not found for deletion");
        }
        showtimeRepository.deleteById(id);
    }

    // --- Helper function: Mapping từ Entity sang Response DTO ---
    private ShowtimeResponse mapToShowtimeResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .theaterName(showtime.getTheater().getName()) // Lấy tên Theater
                .roomId(showtime.getRoom().getId()) // Lấy ID Room
                .roomName(showtime.getRoom().getName()) // Lấy tên Room
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .build();
    }

    // --- Helper function: Kiểm tra trùng lịch ---
    private void checkOverlap(UUID roomId, LocalDateTime newStartTime, LocalDateTime newEndTime,
            UUID excludedShowtimeId) {
        List<Showtime> overlappingShowtimes = showtimeRepository.findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                roomId,
                newStartTime,
                newEndTime);

        if (!overlappingShowtimes.isEmpty()) {
            // Trong trường hợp Update, ta loại trừ chính suất chiếu đang được update
            if (excludedShowtimeId != null) {
                overlappingShowtimes.removeIf(st -> st.getId().equals(excludedShowtimeId));
            }

            if (!overlappingShowtimes.isEmpty()) {
                throw new IllegalStateException("Showtime overlaps with an existing showtime in Room ID " + roomId);
            }
        }
    }
}