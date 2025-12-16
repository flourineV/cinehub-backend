package com.cinehub.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorite_movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavoriteMovie {

    @EmbeddedId
    private FavoriteMovieId id;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
