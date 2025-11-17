package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateShowtimeRequest {
    private UUID roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID excludeShowtimeId; // Optional: for update validation
}
