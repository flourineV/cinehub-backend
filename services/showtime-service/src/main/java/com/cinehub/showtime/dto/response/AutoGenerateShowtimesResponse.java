package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoGenerateShowtimesResponse {
    private int totalGenerated;
    private int totalSkipped;
    private List<String> generatedMovies;
    private List<String> skippedMovies;
    private List<String> errors;
    private String message;
}
