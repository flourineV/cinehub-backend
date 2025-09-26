package com.cinehub.showtime.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class ShowtimeRequest {
    private String movieId;    
    private String theaterId;
    private String roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
}
