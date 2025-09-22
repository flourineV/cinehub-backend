package com.cinehub.movie.dto.TMDb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TMDbReleaseDatesResponse {
    private Integer id;
    private List<Result> results;

    @Data
    public static class Result {
        @JsonProperty("iso_3166_1")
        private String iso31661;

        @JsonProperty("release_dates")
        private List<ReleaseDate> releaseDates;
    }

    @Data
    public static class ReleaseDate {
        private String certification;
        private List<String> descriptors;

        @JsonProperty("release_date")
        private String releaseDate;

        private Integer type; // 3 = theatrical
    }
}
