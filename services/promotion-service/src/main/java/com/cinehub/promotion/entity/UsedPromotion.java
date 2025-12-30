package com.cinehub.promotion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "used_promotion", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_user_promotion_code", 
           columnNames = {"user_id", "promotion_code"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsedPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "promotion_code", nullable = false, length = 50)
    private String promotionCode;

    @Column(name = "booking_id")
    private UUID bookingId;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
}
