package com.cinehub.profile.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteMovieId implements Serializable {
    private UUID userId;
    private Integer tmdbId;
}
