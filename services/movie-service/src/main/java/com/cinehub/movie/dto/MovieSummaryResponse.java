package com.cinehub.movie.dto;

import com.cinehub.movie.entity.MovieStatus;
import java.time.LocalDate;
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
    private MovieStatus status;
    private Integer time;
    private List<String> spokenLanguages;
    private String country;
    private List<String> genres;
    private String trailer;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double popularity;
}
