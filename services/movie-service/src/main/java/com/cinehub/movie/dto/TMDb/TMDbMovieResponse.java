package com.cinehub.movie.dto.TMDb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TMDbMovieResponse {
    private boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    private Integer budget;
    private List<Genre> genres;
    private Integer id;

    @JsonProperty("imdb_id")
    private String imdbId;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;
    private Double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;

    private Long revenue;
    private Integer runtime;
    private String status;
    private String tagline;
    private String title;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("production_countries")
    private List<ProductionCountry> productionCountries;

    @JsonProperty("spoken_languages")
    private List<SpokenLanguage> spokenLanguages;

    @Data
    public static class Genre {
        private Integer id;
        private String name;
    }

    @Data
    public static class ProductionCountry {
        @JsonProperty("iso_3166_1")
        private String iso31661;
        private String name;
    }

    @Data
    public static class SpokenLanguage {
        @JsonProperty("english_name")
        private String englishName;
        @JsonProperty("iso_639_1")
        private String iso6391;
        private String name;
    }
}
