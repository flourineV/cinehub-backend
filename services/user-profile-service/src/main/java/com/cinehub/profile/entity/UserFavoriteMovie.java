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

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_favorite_user"))
    private UserProfile user;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
