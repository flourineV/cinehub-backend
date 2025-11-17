package com.cinehub.showtime.service;

import com.cinehub.showtime.client.MovieServiceClient;
import com.cinehub.showtime.client.MovieSummaryResponse;
import com.cinehub.showtime.dto.request.BatchShowtimeRequest;
import com.cinehub.showtime.dto.request.ShowtimeRequest;
import com.cinehub.showtime.dto.request.ValidateShowtimeRequest;
import com.cinehub.showtime.dto.response.AutoGenerateShowtimesResponse;
import com.cinehub.showtime.dto.response.BatchShowtimeResponse;
import com.cinehub.showtime.dto.response.PagedResponse;
import com.cinehub.showtime.dto.response.ShowtimeConflictResponse;
import com.cinehub.showtime.dto.response.ShowtimeDetailResponse;
import com.cinehub.showtime.dto.response.ShowtimeResponse;
import com.cinehub.showtime.dto.response.ShowtimesByMovieResponse;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.repository.SeatRepository;
import com.cinehub.showtime.repository.ShowtimeRepository;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import com.cinehub.showtime.repository.TheaterRepository;
import com.cinehub.showtime.repository.RoomRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService {

        private final ShowtimeRepository showtimeRepository;
        private final TheaterRepository theaterRepository;
        private final RoomRepository roomRepository;
        private final SeatRepository seatRepository;
        private final ShowtimeSeatRepository showtimeSeatRepository;
        private final MovieServiceClient movieServiceClient;

        public ShowtimeResponse createShowtime(ShowtimeRequest request) {
                Theater theater = theaterRepository.findById(request.getTheaterId())
                                .orElseThrow(
                                                () -> new EntityNotFoundException("Theater with ID "
                                                                + request.getTheaterId() + " not found"));
                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Room with ID " + request.getRoomId() + " not found"));

                // 2. KIỂM TRA TRÙNG LỊCH (Gọi hàm helper)
                checkOverlap(request.getRoomId(), request.getStartTime(), request.getEndTime(), null);

                Showtime showtime = Showtime.builder()
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Showtime with ID " + id + " not found"));

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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Showtime with ID " + id + " not found"));

                Theater theater = theaterRepository.findById(request.getTheaterId())
                                .orElseThrow(
                                                () -> new EntityNotFoundException("Theater with ID "
                                                                + request.getTheaterId() + " not found"));

                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Room with ID " + request.getRoomId() + " not found"));

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

        /**
         * Create multiple showtimes at once
         * For admin bulk scheduling
         * Note: Each showtime is saved in its own transaction for safety with
         * skipOnConflict=true
         */
        public BatchShowtimeResponse createShowtimesBatch(BatchShowtimeRequest request) {
                List<ShowtimeResponse> createdShowtimes = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                List<ShowtimeRequest> pendingShowtimes = new ArrayList<>();
                int index = 0;

                for (ShowtimeRequest showtimeRequest : request.getShowtimes()) {
                        index++;
                        try {
                                if (!showtimeRequest.getStartTime().isBefore(showtimeRequest.getEndTime())) {
                                        throw new IllegalArgumentException("startTime must be before endTime");
                                }
                                // Validate entities exist
                                Theater theater = theaterRepository.findById(showtimeRequest.getTheaterId())
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Theater with ID " + showtimeRequest.getTheaterId()
                                                                                + " not found"));
                                Room room = roomRepository.findById(showtimeRequest.getRoomId())
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Room with ID " + showtimeRequest.getRoomId()
                                                                                + " not found"));

                                // 1. Check overlap with existing showtimes in database
                                List<Showtime> overlappingShowtimes = showtimeRepository
                                                .findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                                                                showtimeRequest.getRoomId(),
                                                                showtimeRequest.getStartTime(),
                                                                showtimeRequest.getEndTime());

                                if (!overlappingShowtimes.isEmpty()) {
                                        if (request.isSkipOnConflict()) {
                                                errors.add("Showtime #" + index
                                                                + " skipped: conflicts with existing showtime in database");
                                                continue;
                                        } else {
                                                throw new IllegalStateException(
                                                                "Showtime #" + index
                                                                                + " overlaps with existing showtime in Room ID "
                                                                                + showtimeRequest.getRoomId());
                                        }
                                }

                                // 2. Check overlap with previously processed showtimes in this batch
                                boolean hasInternalConflict = pendingShowtimes.stream()
                                                .anyMatch(pending -> pending.getRoomId()
                                                                .equals(showtimeRequest.getRoomId())
                                                                && overlaps(pending.getStartTime(),
                                                                                pending.getEndTime(),
                                                                                showtimeRequest.getStartTime(),
                                                                                showtimeRequest.getEndTime()));

                                if (hasInternalConflict) {
                                        if (request.isSkipOnConflict()) {
                                                errors.add("Showtime #" + index
                                                                + " skipped: conflicts with previous showtime in batch");
                                                continue;
                                        } else {
                                                throw new IllegalStateException(
                                                                "Showtime #" + index
                                                                                + " overlaps with previous showtime in batch");
                                        }
                                }

                                // Create showtime
                                Showtime showtime = Showtime.builder()
                                                .movieId(showtimeRequest.getMovieId())
                                                .theater(theater)
                                                .room(room)
                                                .startTime(showtimeRequest.getStartTime())
                                                .endTime(showtimeRequest.getEndTime())
                                                .build();

                                Showtime savedShowtime = showtimeRepository.save(showtime);
                                createdShowtimes.add(mapToShowtimeResponse(savedShowtime));

                                // Add to pending list for next iteration's conflict check
                                pendingShowtimes.add(showtimeRequest);

                        } catch (Exception e) {
                                if (request.isSkipOnConflict()) {
                                        errors.add("Showtime #" + index + " failed: " + e.getMessage());
                                } else {
                                        // Fail entire batch if skipOnConflict is false
                                        throw new IllegalStateException(
                                                        "Batch creation failed at showtime #" + index + ": "
                                                                        + e.getMessage(),
                                                        e);
                                }
                        }
                }

                return BatchShowtimeResponse.builder()
                                .totalRequested(request.getShowtimes().size())
                                .successCount(createdShowtimes.size())
                                .failedCount(errors.size())
                                .createdShowtimes(createdShowtimes)
                                .errors(errors)
                                .build();
        }

        /**
         * Get all available showtimes (future showtimes only) with pagination and
         * filters
         * For ADMIN/MANAGER to view in management table
         */
        public PagedResponse<ShowtimeDetailResponse> getAllAvailableShowtimes(
                        UUID provinceId,
                        UUID theaterId,
                        UUID roomId,
                        UUID movieId,
                        UUID showtimeId,
                        int page,
                        int size,
                        String sortBy,
                        String sortType) {

                LocalDateTime now = LocalDateTime.now();

                // Build sort
                Sort sort = Sort.unsorted();
                if (sortBy != null && !sortBy.isEmpty()) {
                        Sort.Direction direction = "desc".equalsIgnoreCase(sortType)
                                        ? Sort.Direction.DESC
                                        : Sort.Direction.ASC;
                        sort = Sort.by(direction, sortBy);
                } else {
                        // Default sort by startTime ascending
                        sort = Sort.by(Sort.Direction.ASC, "startTime");
                }

                Pageable pageable = PageRequest.of(page - 1, size, sort);

                Page<Showtime> showtimePage = showtimeRepository.findAvailableShowtimesWithFilters(
                                now, provinceId, theaterId, roomId, movieId, showtimeId, pageable);

                // Map to detailed response with booking counts
                List<ShowtimeDetailResponse> content = showtimePage.getContent().stream()
                                .map(this::mapToShowtimeDetailResponse)
                                .collect(Collectors.toList());

                return PagedResponse.<ShowtimeDetailResponse>builder()
                                .data(content)
                                .page(page)
                                .size(size)
                                .totalElements(showtimePage.getTotalElements())
                                .totalPages(showtimePage.getTotalPages())
                                .build();
        }

        public List<ShowtimeResponse> getAllAvailableShowtimesSimple() {
                LocalDateTime now = LocalDateTime.now();
                return showtimeRepository.findAll().stream()
                                .filter(showtime -> showtime.getStartTime().isAfter(now))
                                .map(this::mapToShowtimeResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Validate showtime for conflicts before creating/updating
         * Returns conflict information for admin UI
         */
        public ShowtimeConflictResponse validateShowtime(ValidateShowtimeRequest request) {
                List<Showtime> overlappingShowtimes = showtimeRepository.findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                                request.getRoomId(),
                                request.getStartTime(),
                                request.getEndTime());

                // Exclude current showtime if updating
                if (request.getExcludeShowtimeId() != null) {
                        overlappingShowtimes.removeIf(st -> st.getId().equals(request.getExcludeShowtimeId()));
                }

                if (overlappingShowtimes.isEmpty()) {
                        return ShowtimeConflictResponse.builder()
                                        .hasConflict(false)
                                        .message("No conflicts found")
                                        .conflictingShowtimes(List.of())
                                        .build();
                }

                List<ShowtimeResponse> conflicts = overlappingShowtimes.stream()
                                .map(this::mapToShowtimeResponse)
                                .collect(Collectors.toList());

                return ShowtimeConflictResponse.builder()
                                .hasConflict(true)
                                .message("Found " + conflicts.size() + " conflicting showtime(s)")
                                .conflictingShowtimes(conflicts)
                                .build();
        }

        /**
         * Get showtimes by room and date range for admin scheduling view
         */
        public List<ShowtimeResponse> getShowtimesByRoomAndDateRange(UUID roomId, LocalDateTime start,
                        LocalDateTime end) {
                return showtimeRepository.findByRoomIdAndEndTimeAfterAndStartTimeBefore(roomId, start, end).stream()
                                .map(this::mapToShowtimeResponse)
                                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                                .collect(Collectors.toList());
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

        /**
         * Map to detailed response with booking information
         */
        private ShowtimeDetailResponse mapToShowtimeDetailResponse(Showtime showtime) {
                // Get total seats for this room
                int totalSeats = seatRepository.countByRoomId(showtime.getRoom().getId());

                // Get booked seats count
                long bookedSeats = showtimeSeatRepository.countBookedSeatsByShowtimeId(showtime.getId());

                // Fetch movie title from movie-service
                String movieTitle = movieServiceClient.getMovieTitle(showtime.getMovieId());

                return ShowtimeDetailResponse.builder()
                                .id(showtime.getId())
                                .movieId(showtime.getMovieId())
                                .movieTitle(movieTitle)
                                .theaterId(showtime.getTheater().getId())
                                .theaterName(showtime.getTheater().getName())
                                .provinceId(showtime.getTheater().getProvince().getId())
                                .provinceName(showtime.getTheater().getProvince().getName())
                                .roomId(showtime.getRoom().getId())
                                .roomName(showtime.getRoom().getName())
                                .startTime(showtime.getStartTime())
                                .endTime(showtime.getEndTime())
                                .totalSeats(totalSeats)
                                .bookedSeats((int) bookedSeats)
                                .availableSeats(totalSeats - (int) bookedSeats)
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
                                throw new IllegalStateException(
                                                "Showtime overlaps with an existing showtime in Room ID " + roomId);
                        }
                }
        }

        /**
         * Check if two time ranges overlap
         */
        private boolean overlaps(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
                return start1.isBefore(end2) && end1.isAfter(start2);
        }

        /**
         * Auto-generate showtimes for all available movies for the next 3 days
         * Only generates for movies that will be NOW_PLAYING during these days
         * Schedules from 5:00 AM to 12:00 AM with 20-minute cleaning gap between
         * showtimes
         * Uses weighted distribution based on movie popularity
         */
        public AutoGenerateShowtimesResponse autoGenerateShowtimes() {
                log.info("Starting auto-generation of showtimes for next 3 days");

                LocalDate today = LocalDate.now();
                LocalDate endDate = today.plusDays(3);

                // Get all available movies from movie-service
                List<MovieSummaryResponse> availableMovies = movieServiceClient
                                .getAvailableMoviesForDateRange(today, endDate);

                if (availableMovies.isEmpty()) {
                        return AutoGenerateShowtimesResponse.builder()
                                        .totalGenerated(0)
                                        .totalSkipped(0)
                                        .generatedMovies(List.of())
                                        .skippedMovies(List.of())
                                        .errors(List.of())
                                        .message("No available movies found for the next 3 days")
                                        .build();
                }

                log.info("Found {} available movies for next 3 days", availableMovies.size());

                // Get all theaters and their rooms
                List<Theater> theaters = theaterRepository.findAll();

                int totalGenerated = 0;
                int totalSkipped = 0;
                List<String> generatedMovies = new ArrayList<>();
                List<String> skippedMovies = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                // Operating hours: 5:00 AM to 12:00 AM (midnight)
                int startHour = 5;
                int endHour = 24;
                int cleaningGapMinutes = 20;

                // Generate for each day
                for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
                        LocalDate targetDate = today.plusDays(dayOffset);

                        // Filter movies available on this day
                        List<MovieSummaryResponse> todayMovies = availableMovies.stream()
                                        .filter(m -> isMovieAvailableOnDate(m, targetDate))
                                        .toList();

                        if (todayMovies.isEmpty()) {
                                continue;
                        }

                        // Create weighted movie pool based on popularity
                        List<MovieSummaryResponse> weightedMoviePool = createWeightedMoviePool(todayMovies);

                        // For each theater
                        for (Theater theater : theaters) {
                                List<Room> rooms = roomRepository.findByTheaterId(theater.getId());

                                if (rooms.isEmpty()) {
                                        continue;
                                }

                                // For each room, schedule movies throughout the day
                                for (Room room : rooms) {
                                        LocalDateTime currentSlot = targetDate.atTime(startHour, 0);
                                        LocalDateTime dayEnd = targetDate.atTime(endHour, 0);

                                        int poolIndex = 0;

                                        // Fill the day with movies from weighted pool
                                        while (currentSlot.isBefore(dayEnd) && poolIndex < weightedMoviePool.size()) {
                                                MovieSummaryResponse movie = weightedMoviePool.get(poolIndex);

                                                // Calculate showtime duration
                                                int duration = movie.getTime() != null ? movie.getTime() : 120;
                                                LocalDateTime endTime = currentSlot.plusMinutes(duration);

                                                // Check if showtime fits before day end
                                                if (endTime.isAfter(dayEnd)) {
                                                        break; // No more time left today for this room
                                                }

                                                try {
                                                        // Check for conflicts
                                                        List<Showtime> conflicts = showtimeRepository
                                                                        .findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                                                                                        room.getId(), currentSlot,
                                                                                        endTime);

                                                        if (conflicts.isEmpty()) {
                                                                Showtime showtime = Showtime.builder()
                                                                                .movieId(movie.getId())
                                                                                .theater(theater)
                                                                                .room(room)
                                                                                .startTime(currentSlot)
                                                                                .endTime(endTime)
                                                                                .build();

                                                                showtimeRepository.save(showtime);
                                                                totalGenerated++;

                                                                if (!generatedMovies.contains(movie.getTitle())) {
                                                                        generatedMovies.add(movie.getTitle());
                                                                }

                                                                // Move to next slot with cleaning gap
                                                                currentSlot = endTime.plusMinutes(cleaningGapMinutes);
                                                        } else {
                                                                totalSkipped++;
                                                                // Move past the conflict
                                                                currentSlot = currentSlot.plusMinutes(30);
                                                        }
                                                } catch (Exception e) {
                                                        errors.add(String.format(
                                                                        "Failed to generate showtime for movie %s at %s: %s",
                                                                        movie.getTitle(), currentSlot, e.getMessage()));
                                                        log.error("Error generating showtime", e);
                                                        currentSlot = currentSlot.plusMinutes(30);
                                                }

                                                poolIndex++;
                                                // Loop back to start of pool if we run out
                                                if (poolIndex >= weightedMoviePool.size()
                                                                && currentSlot.isBefore(dayEnd)) {
                                                        poolIndex = 0;
                                                }
                                        }
                                }
                        }
                }

                String message = String.format("Generated %d showtimes for %d movies across %d theaters over 3 days",
                                totalGenerated, generatedMovies.size(), theaters.size());

                return AutoGenerateShowtimesResponse.builder()
                                .totalGenerated(totalGenerated)
                                .totalSkipped(totalSkipped)
                                .generatedMovies(generatedMovies)
                                .skippedMovies(skippedMovies)
                                .errors(errors)
                                .message(message)
                                .build();
        }

        /**
         * Create a weighted pool of movies based on popularity
         * Higher popularity = more entries in the pool = more showtimes
         * 
         * Weight formula:
         * - Low popularity (0-50): 1 entry
         * - Medium popularity (50-100): 2 entries
         * - High popularity (100-500): 3 entries
         * - Very high popularity (500+): 5 entries
         */
        private List<MovieSummaryResponse> createWeightedMoviePool(List<MovieSummaryResponse> movies) {
                List<MovieSummaryResponse> weightedPool = new ArrayList<>();

                for (MovieSummaryResponse movie : movies) {
                        double popularity = movie.getPopularity() != null ? movie.getPopularity() : 0.0;
                        int weight;

                        if (popularity >= 500) {
                                weight = 5; // Blockbuster movies
                        } else if (popularity >= 100) {
                                weight = 3; // Popular movies
                        } else if (popularity >= 50) {
                                weight = 2; // Medium popularity
                        } else {
                                weight = 1; // Low popularity
                        }

                        // Add movie multiple times based on weight
                        for (int i = 0; i < weight; i++) {
                                weightedPool.add(movie);
                        }
                }

                log.info("Created weighted pool: {} movies expanded to {} entries",
                                movies.size(), weightedPool.size());

                return weightedPool;
        }

        /**
         * Check if a movie is available on a specific date
         */
        private boolean isMovieAvailableOnDate(MovieSummaryResponse movie, LocalDate date) {
                if (movie.getStartDate() == null) {
                        return false;
                }

                // Movie must have started by this date
                if (movie.getStartDate().isAfter(date)) {
                        return false;
                }

                // If movie has end date, check if it's still showing
                if (movie.getEndDate() != null && movie.getEndDate().isBefore(date)) {
                        return false;
                }

                return true;
        }
}