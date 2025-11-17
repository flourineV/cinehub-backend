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

public class MovieDetailResponse {

    private UUID id;
    private Integer tmdbId;
    private String title;
    private String age;
    private MovieStatus status;
    private String posterUrl;
    private List<String> genres;
    private Integer time;
    private String country;
    private List<String> spokenLanguages;
    private List<String> crew;
    private List<String> cast;
    private String releaseDate;
    private String overview;
    private String trailer;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double popularity;
}
