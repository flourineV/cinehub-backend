package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeStatsResponse {
    private long totalShowtimes;
    private long activeShowtimes;
    private long suspendedShowtimes;
    private long upcomingShowtimes;
}
