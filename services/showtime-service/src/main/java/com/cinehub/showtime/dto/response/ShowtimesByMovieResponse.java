package com.cinehub.showtime.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ShowtimesByMovieResponse {
    private List<LocalDate> availableDates;
    // Key: Ngày chiếu -> Value: Danh sách các Rạp và lịch của họ trong ngày đó
    private Map<LocalDate, List<TheaterScheduleResponse>> scheduleByDate;
}