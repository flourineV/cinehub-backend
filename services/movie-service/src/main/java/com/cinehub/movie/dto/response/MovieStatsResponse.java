package com.cinehub.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieStatsResponse {
    private long totalMovies;
    private long nowPlaying;
    private long upcoming;
    private long archived;
}
