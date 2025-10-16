package com.cinehub.showtime.dto.request;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class ShowtimeRequest {
    private UUID movieId;
    private UUID theaterId;
    private UUID roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
