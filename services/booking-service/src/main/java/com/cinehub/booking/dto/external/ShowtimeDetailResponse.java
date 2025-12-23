package com.cinehub.booking.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ShowtimeDetailResponse {
    private UUID id;
    private UUID movieId;
    private String movieTitle;
    private String movieTitleEn;
    private UUID theaterId;
    private String theaterName;
    private String theaterNameEn;
    private UUID provinceId;
    private String provinceName;
    private UUID roomId;
    private String roomName;
    private String roomNameEn;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalSeats;
    private int bookedSeats;
    private int availableSeats;
}
