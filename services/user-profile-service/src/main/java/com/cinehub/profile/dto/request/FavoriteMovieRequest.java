package com.cinehub.profile.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteMovieRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Movie ID is required")
    private UUID movieId;
}
