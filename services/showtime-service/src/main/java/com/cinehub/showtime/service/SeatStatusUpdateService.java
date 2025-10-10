package com.cinehub.showtime.service;

import com.cinehub.showtime.entity.ShowtimeSeat;
import com.cinehub.showtime.events.BookingStatusUpdatedEvent;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatStatusUpdateService {

    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Transactional
    public void updateSeatStatusFromBooking(BookingStatusUpdatedEvent event) {
        ShowtimeSeat.SeatStatus newStatus = switch (event.status()) {
            case "CONFIRMED" -> ShowtimeSeat.SeatStatus.BOOKED;
            case "CANCELLED" -> ShowtimeSeat.SeatStatus.AVAILABLE;
            default -> null;
        };
        if (newStatus == null) {
            log.warn("⚠️ Unknown booking status: {}", event.status());
            return;
        }

        int updated = showtimeSeatRepository.bulkUpdateSeatStatus(
                event.showtimeId(), event.seatIds(), newStatus, LocalDateTime.now());
        log.info("✅ Bulk updated {} seats for showtime {} → {}", updated, event.showtimeId(), newStatus);
    }
}
