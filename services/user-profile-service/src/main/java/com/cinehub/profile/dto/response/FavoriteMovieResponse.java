package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class FavoriteMovieResponse {
    private UUID movieId;
    private LocalDateTime addedAt;
}
