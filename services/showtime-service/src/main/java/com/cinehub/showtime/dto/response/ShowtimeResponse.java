package com.cinehub.showtime.dto.response;

import java.math.BigDecimal;
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

public class ShowtimeResponse {
    private UUID id;
    private UUID movieId;
    private String theaterName;
    private UUID roomId;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
