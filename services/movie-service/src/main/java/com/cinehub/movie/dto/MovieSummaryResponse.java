package com.cinehub.movie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class MovieSummaryResponse {
    private String id; 
    private Integer tmdbId;               
    private String title;
    private String posterUrl;
    private String age;       
    private String status;
    private Integer time;
    private String language;
    private List<String> genres;
}
