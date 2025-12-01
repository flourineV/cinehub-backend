package com.cinehub.showtime.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TimeSlot implements Comparable<TimeSlot> {
    private LocalDateTime start;
    private LocalDateTime end;

    public long getDurationMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    @Override
    public int compareTo(TimeSlot other) {
        return this.start.compareTo(other.start);
    }
}