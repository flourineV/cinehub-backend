package com.cinehub.showtime.dto.response;

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
public class ShowtimeDetailResponse {
    private UUID id;
    private UUID movieId;
    private String movieTitle;
    private UUID theaterId;
    private String theaterName;
    private String theaterNameEn;
    private UUID provinceId;
    private String provinceName;
    private String provinceNameEn;
    private UUID roomId;
    private String roomName;
    private String roomNameEn;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalSeats;
    private int bookedSeats;
    private int availableSeats;
}
