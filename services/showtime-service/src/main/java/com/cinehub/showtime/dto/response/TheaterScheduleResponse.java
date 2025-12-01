package com.cinehub.showtime.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TheaterScheduleResponse {
    private UUID theaterId;
    private String theaterName;
    private String theaterAddress;
    private List<ShowtimeResponse> showtimes;
}