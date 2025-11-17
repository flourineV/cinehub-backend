package com.cinehub.movie.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Document(collection = "movie_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetail {
    @Id
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
