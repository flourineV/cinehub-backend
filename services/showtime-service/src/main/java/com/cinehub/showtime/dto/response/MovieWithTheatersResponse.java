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
public class MovieWithTheatersResponse {
    private UUID movieId;
    private List<TheaterWithShowtimes> theaters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TheaterWithShowtimes {
        private UUID theaterId;
        private String theaterName;
        private String theaterNameEn;
        private String theaterAddress;
        private String theaterAddressEn;
        private List<ShowtimeDetail> showtimes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowtimeDetail {
        private UUID showtimeId;
        private String roomName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
