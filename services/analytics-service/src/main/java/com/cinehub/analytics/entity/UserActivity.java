package com.cinehub.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType; // VIEW_MOVIE, SEARCH, BOOK_TICKET, VIEW_SHOWTIME, etc.

    @Column(name = "entity_type", length = 50)
    private String entityType; // MOVIE, SHOWTIME, BOOKING, etc.

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
