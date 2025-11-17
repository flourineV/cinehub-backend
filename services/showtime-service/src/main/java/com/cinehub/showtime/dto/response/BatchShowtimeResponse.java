package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchShowtimeResponse {
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<ShowtimeResponse> createdShowtimes;
    private List<String> errors;
}
