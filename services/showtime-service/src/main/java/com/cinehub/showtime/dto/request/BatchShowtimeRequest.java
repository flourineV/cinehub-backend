package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchShowtimeRequest {
    private List<ShowtimeRequest> showtimes;
    private boolean skipOnConflict; // true: skip conflicting ones, false: fail entire batch
}
