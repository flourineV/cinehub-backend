package com.cinehub.profile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Email
    @NotBlank
    @Size(max = 100)
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Size(max = 30)
    @Column(name = "username", unique = true)
    private String username;

    @Size(max = 100)
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 20)
    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "loyalty_point")
    @Builder.Default
    private Integer loyaltyPoint = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", foreignKey = @ForeignKey(name = "fk_user_rank"))
    private UserRank rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum UserStatus {
        ACTIVE, BANNED
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
