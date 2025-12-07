package com.cinehub.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_loyalty_user"))
    private UserProfile user;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "points_change", nullable = false)
    private Integer pointsChange;

    @Column(name = "points_before", nullable = false)
    private Integer pointsBefore;

    @Column(name = "points_after", nullable = false)
    private Integer pointsAfter;

    @Column(name = "amount_spent")
    private BigDecimal amountSpent;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        EARN,       // Kiếm điểm từ booking
        REDEEM,     // Đổi điểm lấy voucher
        REFUND,     // Hoàn điểm khi refund booking
        ADJUSTMENT  // Admin điều chỉnh
    }
}
