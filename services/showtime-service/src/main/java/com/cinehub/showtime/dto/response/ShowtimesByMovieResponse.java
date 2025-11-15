package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimesByMovieResponse {

    private List<LocalDate> availableDates; // Danh sách các ngày có lịch chiếu
    private Map<LocalDate, List<ShowtimeResponse>> showtimesByDate; // Showtimes grouped by date
}
