package com.cinehub.booking.client;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeResponse {
    private String id;
    private String movieId;
    private String theaterId;
    private String roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
}
