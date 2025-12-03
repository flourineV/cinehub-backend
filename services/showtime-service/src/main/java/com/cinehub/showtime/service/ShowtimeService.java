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
import com.cinehub.showtime.dto.response.TheaterScheduleResponse;
import com.cinehub.showtime.dto.response.TheaterShowtimesResponse;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.repository.ShowtimeRepository;
import com.cinehub.showtime.repository.ShowtimeRepositoryCustom;
import com.cinehub.showtime.repository.TheaterRepository;
import com.cinehub.showtime.repository.RoomRepository;
import com.cinehub.showtime.mapper.ShowtimeMapper;
import com.cinehub.showtime.dto.model.GenerationStats;
import com.cinehub.showtime.helper.ShowtimeGenerationHelper;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        private final ShowtimeRepositoryCustom showtimeRepositoryCustom;
        private final MovieServiceClient movieServiceClient;

        private final ShowtimeMapper showtimeMapper;
        private final ShowtimeGenerationHelper generationHelper;

        public ShowtimeResponse createShowtime(ShowtimeRequest request) {
                Theater theater = theaterRepository.findById(request.getTheaterId())
                                .orElseThrow(() -> new EntityNotFoundException("Theater not found"));
                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

                checkOverlap(request.getRoomId(), request.getStartTime(), request.getEndTime(), null);

                Showtime showtime = Showtime.builder()
                                .movieId(request.getMovieId())
                                .theater(theater)
                                .room(room)
                                .startTime(request.getStartTime())
                                .endTime(request.getEndTime())
                                .status(com.cinehub.showtime.entity.ShowtimeStatus.ACTIVE)
                                .build();

                Showtime saved = showtimeRepository.save(showtime);

                // Cập nhật movie thành NOW_PLAYING nếu đang UPCOMING
                try {
                        movieServiceClient.updateMovieToNowPlaying(request.getMovieId());
                } catch (Exception e) {
                        log.warn("Failed to update movie {} to NOW_PLAYING", request.getMovieId(), e);
                }

                return showtimeMapper.toShowtimeResponse(saved);
        }

        public ShowtimeResponse updateShowtime(UUID id, ShowtimeRequest request) {
                Showtime showtime = showtimeRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
                Theater theater = theaterRepository.findById(request.getTheaterId())
                                .orElseThrow(() -> new EntityNotFoundException("Theater not found"));
                Room room = roomRepository.findById(request.getRoomId())
                                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

                checkOverlap(request.getRoomId(), request.getStartTime(), request.getEndTime(), id);

                showtime.setMovieId(request.getMovieId());
                showtime.setTheater(theater);
                showtime.setRoom(room);
                showtime.setStartTime(request.getStartTime());
                showtime.setEndTime(request.getEndTime());

                return showtimeMapper.toShowtimeResponse(showtimeRepository.save(showtime));
        }

        public void deleteShowtime(UUID id) {
                if (!showtimeRepository.existsById(id)) {
                        throw new EntityNotFoundException("Showtime not found");
                }
                showtimeRepository.deleteById(id);
        }

        public ShowtimeResponse getShowtimeById(UUID id) {
                Showtime showtime = showtimeRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
                return showtimeMapper.toShowtimeResponse(showtime);
        }

        public List<ShowtimeResponse> getAllShowtimes() {
                return showtimeRepository.findAll().stream()
                                .map(showtimeMapper::toShowtimeResponse)
                                .collect(Collectors.toList());
        }

        public List<ShowtimeResponse> getShowtimesByTheaterAndDate(UUID theaterId, LocalDateTime start,
                        LocalDateTime end) {
                return showtimeRepository.findByTheaterIdAndStartTimeBetween(theaterId, start, end).stream()
                                .map(showtimeMapper::toShowtimeResponse)
                                .collect(Collectors.toList());
        }

        public List<ShowtimeResponse> getShowtimesByMovie(UUID movieId) {
                return showtimeRepository.findByMovieId(movieId).stream()
                                .map(showtimeMapper::toShowtimeResponse)
                                .collect(Collectors.toList());
        }

        public ShowtimesByMovieResponse getShowtimesByMovieGrouped(UUID movieId) {
                LocalDate today = LocalDate.now();
                List<LocalDate> targetDates = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        targetDates.add(today.plusDays(i));
                }

                List<Showtime> allShowtimes = showtimeRepository.findByMovieId(movieId).stream()
                                .filter(st -> !st.getStartTime().toLocalDate().isBefore(today))
                                .collect(Collectors.toList());

                List<Theater> allTheaters = theaterRepository.findAll();

                Map<LocalDate, Map<UUID, List<Showtime>>> groupedData = allShowtimes.stream()
                                .collect(Collectors.groupingBy(
                                                st -> st.getStartTime().toLocalDate(),
                                                Collectors.groupingBy(st -> st.getTheater().getId())));

                Map<LocalDate, List<TheaterScheduleResponse>> scheduleByDate = new LinkedHashMap<>();

                for (LocalDate date : targetDates) {
                        Map<UUID, List<Showtime>> showtimesOnDate = groupedData.getOrDefault(date, new HashMap<>());
                        List<TheaterScheduleResponse> dailySchedules = allTheaters.stream()
                                        .map(theater -> {
                                                List<Showtime> theaterShowtimes = showtimesOnDate
                                                                .getOrDefault(theater.getId(), new ArrayList<>());
                                                List<ShowtimeResponse> showtimeDtos = theaterShowtimes.stream()
                                                                .sorted(Comparator.comparing(Showtime::getStartTime))
                                                                .map(showtimeMapper::toShowtimeResponse)
                                                                .collect(Collectors.toList());

                                                return TheaterScheduleResponse.builder()
                                                                .theaterId(theater.getId())
                                                                .theaterName(theater.getName())
                                                                .theaterAddress(theater.getAddress())
                                                                .showtimes(showtimeDtos)
                                                                .build();
                                        })
                                        .collect(Collectors.toList());
                        scheduleByDate.put(date, dailySchedules);
                }

                return ShowtimesByMovieResponse.builder()
                                .availableDates(targetDates)
                                .scheduleByDate(scheduleByDate)
                                .build();
        }

        public List<com.cinehub.showtime.dto.response.MovieShowtimesResponse> getMoviesByTheater(UUID theaterId) {
                LocalDateTime now = LocalDateTime.now();
                List<Showtime> showtimes = showtimeRepository.findByTheaterIdAndStartTimeAfter(theaterId, now);

                // Group by movieId
                java.util.Map<UUID, List<Showtime>> showtimesByMovie = showtimes.stream()
                                .collect(java.util.stream.Collectors.groupingBy(Showtime::getMovieId));

                // Map to response
                return showtimesByMovie.entrySet().stream()
                                .map(entry -> {
                                        UUID movieId = entry.getKey();
                                        List<Showtime> movieShowtimes = entry.getValue();

                                        List<com.cinehub.showtime.dto.response.MovieShowtimesResponse.ShowtimeInfo> showtimeInfos = movieShowtimes
                                                        .stream()
                                                        .map(s -> com.cinehub.showtime.dto.response.MovieShowtimesResponse.ShowtimeInfo
                                                                        .builder()
                                                                        .showtimeId(s.getId())
                                                                        .roomId(s.getRoom().getId())
                                                                        .roomName(s.getRoom().getName())
                                                                        .startTime(s.getStartTime())
                                                                        .endTime(s.getEndTime())
                                                                        .status(s.getStatus().toString())
                                                                        .build())
                                                        .toList();

                                        return com.cinehub.showtime.dto.response.MovieShowtimesResponse.builder()
                                                        .movieId(movieId)
                                                        .showtimes(showtimeInfos)
                                                        .build();
                                })
                                .toList();
        }

        public List<TheaterShowtimesResponse> getTheaterShowtimesByMovieAndProvince(UUID movieId, UUID provinceId) {
                LocalDateTime now = LocalDateTime.now();
                List<Showtime> showtimes = showtimeRepository.findByMovieAndProvince(movieId, provinceId, now);

                Map<UUID, List<Showtime>> showtimesByTheater = showtimes.stream()
                                .collect(Collectors.groupingBy(s -> s.getTheater().getId()));

                return showtimesByTheater.entrySet().stream()
                                .map(entry -> {
                                        UUID theaterId = entry.getKey();
                                        List<Showtime> theaterShowtimes = entry.getValue();
                                        Theater theater = theaterShowtimes.get(0).getTheater();

                                        List<TheaterShowtimesResponse.ShowtimeInfo> showtimeInfos = theaterShowtimes
                                                        .stream()
                                                        .sorted(Comparator.comparing(Showtime::getStartTime))
                                                        .map(showtimeMapper::toShowtimeInfo)
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

        public PagedResponse<ShowtimeDetailResponse> getAllAvailableShowtimes(
                        UUID provinceId, UUID theaterId, UUID roomId, UUID movieId, UUID showtimeId,
                        LocalDate selectedDate, LocalDateTime startOfDay, LocalDateTime endOfDay,
                        LocalTime fromTime, LocalTime toTime, int page, int size, String sortBy, String sortType) {

                Sort sort = Sort.unsorted();
                if (sortBy != null && !sortBy.isEmpty()) {
                        Sort.Direction direction = "desc".equalsIgnoreCase(sortType) ? Sort.Direction.DESC
                                        : Sort.Direction.ASC;
                        sort = Sort.by(direction, sortBy);
                } else {
                        sort = Sort.by(Sort.Direction.ASC, "startTime");
                }

                Pageable pageable = PageRequest.of(page - 1, size, sort);
                Page<Showtime> showtimePage = showtimeRepositoryCustom.findAvailableShowtimesWithFiltersDynamic(
                                provinceId, theaterId, roomId, movieId, showtimeId, selectedDate, startOfDay,
                                endOfDay, fromTime, toTime, LocalDateTime.now(), pageable);

                List<ShowtimeDetailResponse> content = showtimePage.getContent().stream()
                                .map(showtimeMapper::toShowtimeDetailResponse)
                                .collect(Collectors.toList());

                return PagedResponse.<ShowtimeDetailResponse>builder()
                                .data(content)
                                .page(page)
                                .size(size)
                                .totalElements(showtimePage.getTotalElements())
                                .totalPages(showtimePage.getTotalPages())
                                .build();
        }

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
                                                .findOverlappingShowtimes(
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

                                // Cập nhật movie thành NOW_PLAYING
                                try {
                                        movieServiceClient.updateMovieToNowPlaying(showtimeRequest.getMovieId());
                                } catch (Exception ex) {
                                        log.warn("Failed to update movie {} to NOW_PLAYING",
                                                        showtimeRequest.getMovieId(), ex);
                                }

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

        public ShowtimeConflictResponse validateShowtime(ValidateShowtimeRequest request) {
                List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
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
                                .status(showtime.getStatus() != null ? showtime.getStatus().name() : "ACTIVE")
                                .build();
        }

        // --- Helper function: Kiểm tra trùng lịch ---
        private void checkOverlap(UUID roomId, LocalDateTime newStartTime, LocalDateTime newEndTime,
                        UUID excludedShowtimeId) {
                List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
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
                log.info("Starting auto-generation from {} to {}", startDate, endDate);

                List<MovieSummaryResponse> availableMovies = movieServiceClient
                                .getAvailableMoviesForDateRange(startDate, endDate);
                if (availableMovies.isEmpty())
                        return buildEmptyResponse("No movies found");

                List<Theater> theaters = theaterRepository.findAll();
                if (theaters.isEmpty())
                        return buildEmptyResponse("No theaters found");

                GenerationStats stats = new GenerationStats();
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

                for (long i = 0; i < daysBetween; i++) {
                        LocalDate targetDate = startDate.plusDays(i);
                        generateForDate(targetDate, availableMovies, theaters, stats);
                }

                return buildSuccessResponse(stats, theaters.size(), daysBetween);
        }

        private void generateForDate(LocalDate date, List<MovieSummaryResponse> movies, List<Theater> theaters,
                        GenerationStats stats) {
                List<MovieSummaryResponse> todayMovies = movies.stream()
                                .filter(m -> isMovieAvailable(m, date))
                                .toList();
                if (todayMovies.isEmpty())
                        return;

                List<MovieSummaryResponse> weightedPool = createWeightedMoviePool(todayMovies);

                for (Theater t : theaters) {
                        List<Room> rooms = roomRepository.findByTheaterId(t.getId());
                        // Bỏ ensureOneShowtimePerMovie - weighted pool đã đảm bảo phim popular xuất
                        // hiện nhiều
                        for (Room r : rooms) {
                                generationHelper.generateForRoom(date, t, r, weightedPool, stats);
                        }
                }
        }

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

        private List<MovieSummaryResponse> createWeightedMoviePool(List<MovieSummaryResponse> movies) {
                if (movies.isEmpty())
                        return new ArrayList<>();

                List<MovieSummaryResponse> pool = new ArrayList<>();

                // Log để debug dải điểm thực tế
                double maxPop = movies.stream()
                                .mapToDouble(m -> m.getPopularity() != null ? m.getPopularity() : 0)
                                .max().orElse(0);
                log.info("Generating pool. Max popularity in batch: {}", maxPop);

                for (MovieSummaryResponse m : movies) {
                        double pop = m.getPopularity() != null ? m.getPopularity() : 5.0;

                        // GỌI HÀM MỚI Ở ĐÂY
                        int weight = calculateDynamicWeight(pop);

                        // Log chi tiết để bạn kiểm tra
                        // log.debug("Movie: {} | Pop: {} | Weight: {}", m.getTitle(), pop, weight);

                        for (int i = 0; i < weight; i++) {
                                pool.add(m);
                        }
                }

                // Đừng quên shuffle!
                Collections.shuffle(pool);
                return pool;
        }

        private int calculateDynamicWeight(double popularity) {
                // Nhóm Siêu Bom Tấn (Marvel, Avatar...)
                if (popularity >= 18.0)
                        return 12; // Gấp 12 lần phim thường
                if (popularity >= 15.0)
                        return 10;

                // Nhóm Bom Tấn
                if (popularity >= 12.0)
                        return 8;
                if (popularity >= 10.0)
                        return 6;

                // Nhóm Phổ biến
                if (popularity >= 8.0)
                        return 5;
                if (popularity >= 6.0)
                        return 4;

                // Nhóm Trung bình
                if (popularity >= 4.0)
                        return 3;

                // Nhóm Ít người xem
                if (popularity >= 2.0)
                        return 2;

                return 1;
        }

        private boolean isMovieAvailable(MovieSummaryResponse m, LocalDate d) {
                if (m.getStartDate() == null)
                        return false;
                if (m.getStartDate().isAfter(d))
                        return false;
                return m.getEndDate() == null || !m.getEndDate().isBefore(d);
        }

        private AutoGenerateShowtimesResponse buildSuccessResponse(GenerationStats stats, int theaterCount,
                        long dayCount) {
                return AutoGenerateShowtimesResponse.builder()
                                .totalGenerated(stats.getTotalGenerated())
                                .totalSkipped(stats.getTotalSkipped())
                                .generatedMovies(stats.getGeneratedMovies())
                                .message(String.format("Generated %d showtimes across %d theaters in %d days",
                                                stats.getTotalGenerated(), theaterCount, dayCount))
                                .build();
        }
}
