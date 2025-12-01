package com.cinehub.showtime.dto.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class GenerationStats {
    private int totalGenerated = 0;
    private int totalSkipped = 0;
    private List<String> generatedMovies = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}