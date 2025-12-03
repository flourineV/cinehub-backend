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
public class MovieShowtimesResponse {
    private UUID movieId;
    private List<ShowtimeInfo> showtimes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowtimeInfo {
        private UUID showtimeId;
        private UUID roomId;
        private String roomName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }
}
