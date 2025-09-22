package com.cinehub.movie.dto.TMDb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TMDbCreditsResponse {
    private List<Cast> cast;
    private List<Crew> crew;

    @Data
    public static class Cast {
        private Integer id;
        private String name;
        private String character;

        @JsonProperty("profile_path")
        private String profilePath;
    }

    @Data
    public static class Crew {
        private Integer id;
        private String name;
        private String job;        // Director, Writer,...
        private String department; // Directing, Writing,...
    }
}
