package com.cinehub.movie.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class MovieSummaryResponse {
    private UUID id;
    private Integer tmdbId;
    private String title;
    private String posterUrl;
    private String age;
    private String status;
    private Integer time;
    private List<String> spokenLanguages;
    private List<String> genres;
    private String trailer;
}
