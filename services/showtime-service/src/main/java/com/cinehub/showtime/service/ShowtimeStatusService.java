package com.cinehub.showtime.service;

import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.entity.ShowtimeStatus;
import com.cinehub.showtime.events.ShowtimeSuspendedEvent;
import com.cinehub.showtime.producer.ShowtimeProducer;
import com.cinehub.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeStatusService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeProducer showtimeProducer;

    /**
     * Suspend all active showtimes for a movie (when movie is archived)
     */
    @Transactional
    public int suspendShowtimesByMovie(UUID movieId, String reason) {
        // Find all ACTIVE showtimes for this movie in the future
        List<Showtime> showtimes = showtimeRepository.findByMovieIdAndStatusAndStartTimeAfter(
            movieId, 
            ShowtimeStatus.ACTIVE, 
            LocalDateTime.now()
        );

        if (showtimes.isEmpty()) {
            log.info("No active showtimes found for movie {}", movieId);
            return 0;
        }

        int count = 0;
        for (Showtime showtime : showtimes) {
            showtime.setStatus(ShowtimeStatus.SUSPENDED);
            showtimeRepository.save(showtime);

            // Send event to booking service to refund users
            ShowtimeSuspendedEvent event = new ShowtimeSuspendedEvent(
                showtime.getId(),
                movieId,
                List.of(), // Booking service will find affected bookings
                reason
            );
            showtimeProducer.sendShowtimeSuspendedEvent(event);

            log.info("Showtime {} suspended. Reason: {}", showtime.getId(), reason);
            count++;
        }

        log.info("Suspended {} showtimes for movie {}. Reason: {}", count, movieId, reason);
        return count;
    }

    /**
     * Suspend a specific showtime
     */
    @Transactional
    public void suspendShowtime(UUID showtimeId, String reason) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
            .orElseThrow(() -> new RuntimeException("Showtime not found: " + showtimeId));

        if (showtime.getStatus() == ShowtimeStatus.SUSPENDED) {
            throw new RuntimeException("Showtime already suspended");
        }

        showtime.setStatus(ShowtimeStatus.SUSPENDED);
        showtimeRepository.save(showtime);

        // Send event
        ShowtimeSuspendedEvent event = new ShowtimeSuspendedEvent(
            showtimeId,
            showtime.getMovieId(),
            List.of(),
            reason
        );
        showtimeProducer.sendShowtimeSuspendedEvent(event);

        log.info("Showtime {} suspended. Reason: {}", showtimeId, reason);
    }
}
