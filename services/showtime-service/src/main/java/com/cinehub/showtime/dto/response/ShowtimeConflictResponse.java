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
public class ShowtimeConflictResponse {
    private boolean hasConflict;
    private String message;
    private List<ShowtimeResponse> conflictingShowtimes;
}
