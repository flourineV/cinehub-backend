package com.cinehub.movie.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = "movie_translations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "movie_lang_idx", def = "{'movieId': 1, 'language': 1}", unique = true)
public class MovieTranslation {

    @Id
    private UUID id;

    private UUID movieId; // Reference to MovieDetail/MovieSummary

    private String language; // "vi", "en"

    private String title;

    private String overview;

    private List<String> genres;

    private String country;
}
