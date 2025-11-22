package com.cinehub.showtime.service;

import com.cinehub.showtime.client.MovieServiceClient;
import com.cinehub.showtime.client.MovieSummaryResponse;
import com.cinehub.showtime.config.ShowtimeAutoGenerateConfig;
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
import com.cinehub.showtime.dto.response.TheaterShowtimesResponse;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
        private final ShowtimeAutoGenerateConfig autoGenerateConfig;

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
                        LocalDate showDate,
                        LocalTime showTime,
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
                                now, provinceId, theaterId, roomId, movieId, showtimeId, showDate, showTime, pageable);

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

        private boolean overlaps(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
                return start1.isBefore(end2) && end1.isAfter(start2);
        }

        public AutoGenerateShowtimesResponse autoGenerateShowtimes(LocalDate startDate, LocalDate endDate) {
                log.info("Starting auto-generation of showtimes from {} to {}", startDate, endDate);

                // Get all available movies from movie-service
                List<MovieSummaryResponse> availableMovies = movieServiceClient
                                .getAvailableMoviesForDateRange(startDate, endDate);

                if (availableMovies.isEmpty()) {
                        return buildEmptyResponse("No available movies found for the date range");
                }

                log.info("Found {} available movies in date range", availableMovies.size());

                // Get all theaters
                List<Theater> theaters = theaterRepository.findAll();
                if (theaters.isEmpty()) {
                        return buildEmptyResponse("No theaters found in system");
                }

                // Track statistics
                GenerationStats stats = new GenerationStats();

                // Calculate number of days
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

                // Generate for each day in range
                for (long dayOffset = 0; dayOffset < daysBetween; dayOffset++) {
                        LocalDate targetDate = startDate.plusDays(dayOffset);
                        generateShowtimesForDate(targetDate, availableMovies, theaters, stats);
                }

                return buildSuccessResponse(stats, theaters.size(), daysBetween);
        }

        private void generateShowtimesForDate(LocalDate targetDate, List<MovieSummaryResponse> availableMovies,
                        List<Theater> theaters, GenerationStats stats) {

                // Filter movies available on this specific date
                List<MovieSummaryResponse> todayMovies = availableMovies.stream()
                                .filter(m -> isMovieAvailableOnDate(m, targetDate))
                                .toList();

                if (todayMovies.isEmpty()) {
                        log.debug("No movies available for date: {}", targetDate);
                        return;
                }

                // Create weighted movie pool based on popularity
                List<MovieSummaryResponse> weightedMoviePool = createWeightedMoviePool(todayMovies);

                // Generate for each theater
                for (Theater theater : theaters) {
                        generateShowtimesForTheater(targetDate, theater, weightedMoviePool, stats);
                }
        }

        private void generateShowtimesForTheater(LocalDate targetDate, Theater theater,
                        List<MovieSummaryResponse> weightedMoviePool, GenerationStats stats) {

                List<Room> rooms = roomRepository.findByTheaterId(theater.getId());

                if (rooms.isEmpty()) {
                        log.debug("No rooms found for theater: {}", theater.getName());
                        return;
                }

                // Generate for each room
                for (Room room : rooms) {
                        fillRoomWithShowtimes(targetDate, theater, room, weightedMoviePool, stats);
                }
        }

        private void fillRoomWithShowtimes(LocalDate targetDate, Theater theater, Room room,
                        List<MovieSummaryResponse> weightedMoviePool, GenerationStats stats) {

                LocalDateTime currentSlot = targetDate.atTime(autoGenerateConfig.getStartHour(), 0);
                // Handle endHour = 24 as midnight next day
                LocalDateTime dayEnd = autoGenerateConfig.getEndHour() == 24
                                ? targetDate.plusDays(1).atTime(0, 0)
                                : targetDate.atTime(autoGenerateConfig.getEndHour(), 0);

                // PHASE 1: Guarantee each movie at least one showtime
                List<MovieSummaryResponse> uniqueMovies = weightedMoviePool.stream()
                                .distinct()
                                .collect(java.util.stream.Collectors.toList());
                java.util.Collections.shuffle(uniqueMovies); // Random order for fairness

                for (MovieSummaryResponse movie : uniqueMovies) {
                        if (currentSlot.isBefore(dayEnd)) {
                                int duration = movie.getTime() != null ? movie.getTime() : 120;
                                LocalDateTime endTime = currentSlot.plusMinutes(duration);

                                if (endTime.isAfter(dayEnd)) {
                                        break; // No more time
                                }

                                boolean created = tryCreateShowtime(movie, theater, room, currentSlot, endTime, stats);
                                if (created) {
                                        // Round up next slot to nearest 5 minutes
                                        currentSlot = roundUpToNearestInterval(
                                                        endTime.plusMinutes(autoGenerateConfig.getCleaningGapMinutes()),
                                                        5);
                                }
                        }
                }

                // PHASE 2: Fill remaining slots with weighted distribution
                int poolIndex = 0;
                while (currentSlot.isBefore(dayEnd) && !weightedMoviePool.isEmpty()) {
                        MovieSummaryResponse movie = weightedMoviePool.get(poolIndex % weightedMoviePool.size());

                        int duration = movie.getTime() != null ? movie.getTime() : 120;
                        LocalDateTime endTime = currentSlot.plusMinutes(duration);

                        if (endTime.isAfter(dayEnd)) {
                                break;
                        }

                        boolean created = tryCreateShowtime(movie, theater, room, currentSlot, endTime, stats);

                        if (created) {
                                // Round up next slot to nearest 5 minutes
                                currentSlot = roundUpToNearestInterval(
                                                endTime.plusMinutes(autoGenerateConfig.getCleaningGapMinutes()), 5);
                        } else {
                                currentSlot = currentSlot.plusMinutes(30);
                                stats.totalSkipped++;
                        }

                        poolIndex++;
                }
        }

        private boolean tryCreateShowtime(MovieSummaryResponse movie, Theater theater, Room room,
                        LocalDateTime startTime, LocalDateTime endTime, GenerationStats stats) {
                try {
                        // Check for conflicts
                        List<Showtime> conflicts = showtimeRepository
                                        .findByRoomIdAndEndTimeAfterAndStartTimeBefore(room.getId(), startTime,
                                                        endTime);

                        if (!conflicts.isEmpty()) {
                                return false; // Conflict exists, skip
                        }

                        // Create and save showtime
                        Showtime showtime = Showtime.builder()
                                        .movieId(movie.getId())
                                        .theater(theater)
                                        .room(room)
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .build();

                        showtimeRepository.save(showtime);
                        stats.totalGenerated++;

                        // Track unique movie titles
                        if (!stats.generatedMovies.contains(movie.getTitle())) {
                                stats.generatedMovies.add(movie.getTitle());
                                // Update movie status to NOW_PLAYING when first showtime is created
                                movieServiceClient.updateMovieToNowPlaying(movie.getId());
                        }

                        return true;

                } catch (Exception e) {
                        stats.errors.add(String.format(
                                        "Failed to generate showtime for movie %s at %s: %s",
                                        movie.getTitle(), startTime, e.getMessage()));
                        log.error("Error generating showtime for movie {} at {}", movie.getTitle(), startTime, e);
                        return false;
                }
        }

        private static class GenerationStats {
                int totalGenerated = 0;
                int totalSkipped = 0;
                List<String> generatedMovies = new ArrayList<>();
                List<String> errors = new ArrayList<>();
        }

        // --- Response Builders ---

        private AutoGenerateShowtimesResponse buildEmptyResponse(String message) {
                return AutoGenerateShowtimesResponse.builder()
                                .totalGenerated(0)
                                .totalSkipped(0)
                                .generatedMovies(List.of())
                                .skippedMovies(List.of())
                                .errors(List.of())
                                .message(message)
                                .build();
        }

        private AutoGenerateShowtimesResponse buildSuccessResponse(GenerationStats stats, int theaterCount,
                        long dayCount) {
                String message = String.format(
                                "Generated %d showtimes for %d movies across %d theaters over %d days",
                                stats.totalGenerated, stats.generatedMovies.size(), theaterCount, dayCount);

                return AutoGenerateShowtimesResponse.builder()
                                .totalGenerated(stats.totalGenerated)
                                .totalSkipped(stats.totalSkipped)
                                .generatedMovies(stats.generatedMovies)
                                .skippedMovies(List.of()) // Not used in current implementation
                                .errors(stats.errors)
                                .message(message)
                                .build();
        }

        // --- Algorithm Helpers ---

        // --- Algorithm Helpers ---

        /**
         * Create weighted movie pool based on popularity
         * Higher popularity movies appear multiple times in the pool
         * This ensures they get more showtimes during scheduling
         */
        private List<MovieSummaryResponse> createWeightedMoviePool(List<MovieSummaryResponse> movies) {
                if (movies.isEmpty()) {
                        return new ArrayList<>();
                }

                // Calculate min/max popularity for normalization
                double minPopularity = movies.stream()
                                .map(m -> m.getPopularity() != null ? m.getPopularity() : 0.0)
                                .min(Double::compare)
                                .orElse(0.0);

                double maxPopularity = movies.stream()
                                .map(m -> m.getPopularity() != null ? m.getPopularity() : 0.0)
                                .max(Double::compare)
                                .orElse(100.0);

                List<MovieSummaryResponse> weightedPool = new ArrayList<>();

                for (MovieSummaryResponse movie : movies) {
                        double popularity = movie.getPopularity() != null ? movie.getPopularity() : 0.0;

                        // Normalize to 0-100 scale
                        double normalizedPopularity = normalizePopularity(popularity, minPopularity, maxPopularity);
                        int weight = calculateWeight(normalizedPopularity);

                        log.debug("Movie: {}, Raw popularity: {}, Normalized: {}, Weight: {}",
                                        movie.getTitle(), popularity, normalizedPopularity, weight);

                        // Add movie to pool 'weight' times
                        for (int i = 0; i < weight; i++) {
                                weightedPool.add(movie);
                        }
                }

                log.info("Created weighted pool: {} movies expanded to {} entries (popularity range: {} - {})",
                                movies.size(), weightedPool.size(), minPopularity, maxPopularity);

                return weightedPool;
        }

        /**
         * Normalize popularity to 0-100 scale based on current movie pool
         */
        private double normalizePopularity(double popularity, double min, double max) {
                if (max == min) {
                        return 50.0; // All movies have same popularity, give them middle weight
                }
                return ((popularity - min) / (max - min)) * 100.0;
        }

        /**
         * Calculate weight based on normalized popularity score (0-100)
         * Weight determines how many times a movie appears in the pool
         */
        private int calculateWeight(double normalizedPopularity) {
                if (normalizedPopularity >= 90) {
                        return 8; // Top 10%: 8x showtimes
                } else if (normalizedPopularity >= 75) {
                        return 6; // Top 25%: 6x showtimes
                } else if (normalizedPopularity >= 60) {
                        return 4; // Top 40%: 4x showtimes
                } else if (normalizedPopularity >= 40) {
                        return 3; // Top 60%: 3x showtimes
                } else if (normalizedPopularity >= 20) {
                        return 2; // Top 80%: 2x showtimes
                } else {
                        return 1; // Bottom 20%: 1x showtime
                }
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

        /**
         * Round up time to nearest interval (5 or 10 minutes)
         * Example: 14:23 with interval 5 → 14:25, 14:27 with interval 10 → 14:30
         */
        private LocalDateTime roundUpToNearestInterval(LocalDateTime time, int intervalMinutes) {
                int currentMinute = time.getMinute();
                int remainder = currentMinute % intervalMinutes;

                if (remainder == 0) {
                        return time; // Already aligned
                }

                // Round up to next interval
                int minutesToAdd = intervalMinutes - remainder;
                return time.plusMinutes(minutesToAdd);
        }

        /**
         * Get theaters with their showtimes for a specific movie in a province
         */
        public List<TheaterShowtimesResponse> getTheaterShowtimesByMovieAndProvince(
                        UUID movieId, UUID provinceId) {
                LocalDateTime now = LocalDateTime.now();

                // Query all showtimes for the movie and province
                List<Showtime> showtimes = showtimeRepository.findByMovieAndProvince(movieId, provinceId, now);

                // Group by theater
                Map<UUID, List<Showtime>> showtimesByTheater = showtimes.stream()
                                .collect(Collectors.groupingBy(s -> s.getTheater().getId()));

                // Build response for each theater
                return showtimesByTheater.entrySet().stream()
                                .map(entry -> {
                                        UUID theaterId = entry.getKey();
                                        List<Showtime> theaterShowtimes = entry.getValue();
                                        Theater theater = theaterShowtimes.get(0).getTheater();

                                        // Map showtimes to ShowtimeInfo
                                        List<TheaterShowtimesResponse.ShowtimeInfo> showtimeInfos = theaterShowtimes
                                                        .stream()
                                                        .sorted(Comparator.comparing(Showtime::getStartTime))
                                                        .map(this::mapToShowtimeInfo)
                                                        .collect(Collectors.toList());

                                        return TheaterShowtimesResponse.builder()
                                                        .theaterId(theaterId)
                                                        .theaterName(theater.getName())
                                                        .theaterAddress(theater.getAddress())
                                                        .showtimes(showtimeInfos)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        /**
         * Map Showtime entity to ShowtimeInfo DTO
         */
        private TheaterShowtimesResponse.ShowtimeInfo mapToShowtimeInfo(Showtime showtime) {
                return TheaterShowtimesResponse.ShowtimeInfo.builder()
                                .showtimeId(showtime.getId())
                                .roomId(showtime.getRoom().getId().toString())
                                .roomName(showtime.getRoom().getName())
                                .startTime(showtime.getStartTime())
                                .endTime(showtime.getEndTime())
                                .build();
        }
}
