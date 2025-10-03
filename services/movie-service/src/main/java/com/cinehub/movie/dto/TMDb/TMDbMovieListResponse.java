package com.cinehub.movie.dto.TMDb;

import lombok.Data;

import java.util.List;

@Data
public class TMDbMovieListResponse {
    private int page;
    private List<TMDbMovieResponse> results;
    private int total_pages;
    private int total_results;
}
