package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class FavoriteMovieResponse {
    private Integer tmdbId;
    private LocalDateTime addedAt;
}
