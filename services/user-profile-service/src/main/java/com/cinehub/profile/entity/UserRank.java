package com.cinehub.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_ranks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountRate = BigDecimal.valueOf(0.00);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
