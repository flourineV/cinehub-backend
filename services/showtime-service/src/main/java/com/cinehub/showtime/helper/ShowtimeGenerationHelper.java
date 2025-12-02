package com.cinehub.showtime.helper;

import com.cinehub.showtime.client.MovieServiceClient;
import com.cinehub.showtime.client.MovieSummaryResponse;
import com.cinehub.showtime.config.ShowtimeAutoGenerateConfig;
import com.cinehub.showtime.dto.model.GenerationStats;
import com.cinehub.showtime.dto.model.TimeSlot;
import com.cinehub.showtime.entity.Room;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.Theater;
import com.cinehub.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeGenerationHelper {

    private final ShowtimeRepository showtimeRepository;
    private final MovieServiceClient movieServiceClient;
    private final ShowtimeAutoGenerateConfig config;

    @Transactional
    public void ensureOneShowtimePerMovie(LocalDate date, Theater theater, List<Room> rooms,
            List<MovieSummaryResponse> movies, GenerationStats stats) {
        int roomCount = rooms.size();
        int movieIndex = 0;

        for (MovieSummaryResponse movie : movies) {
            boolean assigned = false;
            // Thử xếp phim vào các phòng lần lượt
            for (int i = 0; i < roomCount; i++) {
                Room room = rooms.get((movieIndex + i) % roomCount);
                if (tryScheduleSingleSlot(date, theater, room, movie, stats)) {
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                log.debug("Could not assign guaranteed slot for movie: {}", movie.getTitle());
            }
            movieIndex++;
        }
    }

    @Transactional
    public void generateForRoom(LocalDate targetDate, Theater theater, Room room,
            List<MovieSummaryResponse> weightedMoviePool, GenerationStats stats) {

        LocalDateTime dayStart = targetDate.atTime(config.getStartHour(), 0);
        LocalDateTime dayEnd = config.getEndHour() == 24
                ? targetDate.plusDays(1).atTime(0, 0)
                : targetDate.atTime(config.getEndHour(), 0);

        List<Showtime> existingShowtimes = showtimeRepository.findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                room.getId(), dayStart.minusHours(4), dayEnd.plusHours(4));
        existingShowtimes.sort(Comparator.comparing(Showtime::getStartTime));

        PriorityQueue<TimeSlot> freeSlotsQueue = new PriorityQueue<>();
        freeSlotsQueue.addAll(calculateFreeSlots(dayStart, dayEnd, existingShowtimes));

        log.info("Room {}: Found {} free slots, {} existing showtimes",
                room.getName(), freeSlotsQueue.size(), existingShowtimes.size());

        int safetyCounter = 0;
        int slotsProcessed = 0;
        int showtimesCreated = 0;

        while (!freeSlotsQueue.isEmpty() && safetyCounter < 500) {
            safetyCounter++;
            TimeSlot currentSlot = freeSlotsQueue.poll();
            slotsProcessed++;

            if (currentSlot.getDurationMinutes() < 60) {
                log.debug("Slot too short: {} minutes", currentSlot.getDurationMinutes());
                continue;
            }

            MovieSummaryResponse selectedMovie = selectMovieStrategy(currentSlot, weightedMoviePool, 0);

            if (selectedMovie != null) {
                int duration = selectedMovie.getTime() != null ? selectedMovie.getTime() : 120;
                LocalDateTime showStart = currentSlot.getStart();
                LocalDateTime showEnd = showStart.plusMinutes(duration);

                createShowtimeEntity(selectedMovie, theater, room, showStart, showEnd, stats);
                showtimesCreated++;

                LocalDateTime nextAvailableStart = roundUpToNearestInterval(
                        showEnd.plusMinutes(config.getCleaningGapMinutes()), 5);

                if (nextAvailableStart.isBefore(currentSlot.getEnd())) {
                    freeSlotsQueue.offer(new TimeSlot(nextAvailableStart, currentSlot.getEnd()));
                }
            } else {
                log.debug("No movie fits slot: {} - {}", currentSlot.getStart(), currentSlot.getEnd());
            }
        }

        log.info("Room {}: Processed {} slots, created {} showtimes",
                room.getName(), slotsProcessed, showtimesCreated);
    }

    private boolean tryScheduleSingleSlot(LocalDate date, Theater theater, Room room,
            MovieSummaryResponse movie, GenerationStats stats) {
        LocalDateTime dayStart = date.atTime(config.getStartHour(), 0);
        LocalDateTime dayEnd = config.getEndHour() == 24
                ? date.plusDays(1).atTime(0, 0)
                : date.atTime(config.getEndHour(), 0);

        List<Showtime> existing = showtimeRepository.findByRoomIdAndEndTimeAfterAndStartTimeBefore(
                room.getId(), dayStart.minusHours(4), dayEnd.plusHours(4));
        existing.sort(Comparator.comparing(Showtime::getStartTime));

        // Dùng thuật toán First Fit đơn giản để tìm slot trống đầu tiên
        List<TimeSlot> slots = calculateFreeSlots(dayStart, dayEnd, existing);
        int duration = movie.getTime() != null ? movie.getTime() : 120;

        for (TimeSlot slot : slots) {
            if (slot.getDurationMinutes() >= duration) {
                createShowtimeEntity(movie, theater, room, slot.getStart(), slot.getStart().plusMinutes(duration),
                        stats);
                return true;
            }
        }
        return false;
    }

    private List<TimeSlot> calculateFreeSlots(LocalDateTime start, LocalDateTime end, List<Showtime> existing) {
        List<TimeSlot> slots = new ArrayList<>();
        if (existing.isEmpty()) {
            slots.add(new TimeSlot(start, end));
            return slots;
        }
        LocalDateTime cursor = start;
        for (Showtime st : existing) {
            if (cursor.isBefore(st.getStartTime())) {
                slots.add(new TimeSlot(cursor, st.getStartTime()));
            }
            LocalDateTime next = st.getEndTime().plusMinutes(config.getCleaningGapMinutes());
            cursor = roundUpToNearestInterval(next, 5);
        }
        if (cursor.isBefore(end)) {
            slots.add(new TimeSlot(cursor, end));
        }
        return slots;
    }

    private MovieSummaryResponse selectMovieStrategy(TimeSlot slot, List<MovieSummaryResponse> pool, int poolIndex) {
        if (isPrimeTime(slot.getStart())) {
            // Giờ vàng: chọn phim có popularity cao nhất
            return findBestPopularMovieForSlot(slot, pool);
        }
        // Giờ thường: random từ weighted pool để phân bố đều
        return findRandomMovieForSlot(slot, pool);
    }

    private boolean isPrimeTime(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= config.getPrimeTimeStartHour() && hour < config.getPrimeTimeEnd();
    }

    private MovieSummaryResponse findBestPopularMovieForSlot(TimeSlot slot, List<MovieSummaryResponse> pool) {
        // Lấy danh sách phim unique và sort theo popularity giảm dần
        List<MovieSummaryResponse> unique = pool.stream().distinct()
                .sorted((m1, m2) -> {
                    Double p1 = m1.getPopularity() != null ? m1.getPopularity() : 0.0;
                    Double p2 = m2.getPopularity() != null ? m2.getPopularity() : 0.0;
                    return p2.compareTo(p1);
                }).toList();

        // Ưu tiên top 3 phim có popularity cao nhất trong giờ vàng
        int topCount = Math.min(3, unique.size());
        for (int i = 0; i < topCount; i++) {
            MovieSummaryResponse topMovie = unique.get(i);
            if (canFit(topMovie, slot))
                return topMovie;
        }

        // Nếu top 3 không fit, thử các phim khác
        for (int i = topCount; i < unique.size(); i++) {
            if (canFit(unique.get(i), slot))
                return unique.get(i);
        }

        return null;
    }

    private MovieSummaryResponse findRandomMovieForSlot(TimeSlot slot, List<MovieSummaryResponse> pool) {
        if (pool.isEmpty())
            return null;

        // Random từ weighted pool - phim có popularity cao xuất hiện nhiều lần hơn
        int randomIndex = (int) (Math.random() * pool.size());
        MovieSummaryResponse randomMovie = pool.get(randomIndex);

        if (canFit(randomMovie, slot))
            return randomMovie;
        return findAny(slot, pool);
    }

    private MovieSummaryResponse findAny(TimeSlot slot, List<MovieSummaryResponse> movies) {
        for (MovieSummaryResponse m : movies) {
            if (canFit(m, slot))
                return m;
        }
        return null;
    }

    private boolean canFit(MovieSummaryResponse m, TimeSlot slot) {
        int d = m.getTime() != null ? m.getTime() : 120;
        return d <= slot.getDurationMinutes();
    }

    private void createShowtimeEntity(MovieSummaryResponse m, Theater t, Room r, LocalDateTime start, LocalDateTime end,
            GenerationStats stats) {
        try {
            Showtime st = Showtime.builder()
                    .movieId(m.getId())
                    .theater(t).room(r)
                    .startTime(start).endTime(end)
                    .status(com.cinehub.showtime.entity.ShowtimeStatus.ACTIVE)
                    .build();
            showtimeRepository.save(st);
            stats.setTotalGenerated(stats.getTotalGenerated() + 1);

            if (!stats.getGeneratedMovies().contains(m.getTitle())) {
                stats.getGeneratedMovies().add(m.getTitle());
                movieServiceClient.updateMovieToNowPlaying(m.getId());
            }
        } catch (Exception e) {
            log.error("Save error: {}", e.getMessage());
            stats.getErrors().add(e.getMessage());
        }
    }

    private LocalDateTime roundUpToNearestInterval(LocalDateTime time, int interval) {
        int min = time.getMinute();
        int rem = min % interval;
        return rem == 0 ? time : time.plusMinutes(interval - rem);
    }
}