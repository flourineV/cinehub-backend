package com.cinehub.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAddMoviesResponse {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<MovieAddResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovieAddResult {
        private Integer tmdbId;
        private boolean success;
        private String message;
        private MovieDetailResponse movieDetail; // null if failed
    }
}
