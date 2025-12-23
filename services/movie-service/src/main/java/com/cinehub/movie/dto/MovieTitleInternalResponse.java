package com.cinehub.movie.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieTitleInternalResponse {
    private UUID id;
    private String title;
    private String titleEn;
}
