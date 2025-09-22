package com.cinehub.movie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class MovieDetailResponse {
    private String id;              
    private Integer tmdbId;
    private String title;
    private String age;
    private List<String> genres;
    private Integer time;           // runtime (minutes)
    private String country;
    private List<String> spokenLanguages;
    private List<String> crew;      // [Director, Writer...]
    private List<String> cast;      // [Actor1, Actor2...]
    private String releaseDate;     // 2025-09-03
    private String overview;
    private String trailer;         // YouTube URL
}
