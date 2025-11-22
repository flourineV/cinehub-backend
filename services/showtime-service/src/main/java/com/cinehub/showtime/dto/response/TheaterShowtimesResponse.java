package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheaterShowtimesResponse {
    private UUID theaterId;
    private String theaterName;
    private String theaterAddress;
    private List<ShowtimeInfo> showtimes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowtimeInfo {
        private UUID showtimeId;
        private String roomId;
        private String roomName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
