package com.cinehub.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "userId"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column
    private UUID bookingId;

    @Column
    private UUID paymentId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type; // PAYMENT_SUCCESS, PROMOTION

    // ✅ Dùng JSONB cho metadata (PostgreSQL)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
