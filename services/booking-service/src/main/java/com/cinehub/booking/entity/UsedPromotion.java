package com.cinehub.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "used_promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UsedPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "promotion_code", nullable = false, length = 50)
    private String promotionCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        this.usedAt = LocalDateTime.now();
    }
}