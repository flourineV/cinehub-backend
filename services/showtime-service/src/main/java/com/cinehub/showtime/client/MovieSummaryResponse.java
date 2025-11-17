package com.cinehub.showtime.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    private Integer time; // duration in minutes
    private List<String> spokenLanguages;
    private List<String> genres;
    private String trailer;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double popularity;
}
