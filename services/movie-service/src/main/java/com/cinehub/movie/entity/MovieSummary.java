package com.cinehub.movie.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "movie_summaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSummary {
    @Id
    private UUID id;
    private Integer tmdbId;
    private String title;
    private String posterUrl;
    private String age;
    private MovieStatus status;
    private List<String> spokenLanguages;
    private String country;
    private Integer time;
    private List<String> genres;
    private String trailer;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double popularity;
}