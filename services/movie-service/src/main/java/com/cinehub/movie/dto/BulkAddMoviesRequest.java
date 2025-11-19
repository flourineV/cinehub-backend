package com.cinehub.movie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddMoviesRequest {

    @NotEmpty(message = "Movies list cannot be empty")
    @Valid
    private List<AddMovieFromTmdbRequest> movies;
}
