package com.cinehub.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "manager_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_manager_user"))
    private UserProfile userProfile;

    @Column(name = "managed_cinema_id")
    private UUID managedCinemaId;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
