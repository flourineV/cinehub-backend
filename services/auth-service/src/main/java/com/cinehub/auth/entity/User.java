package com.cinehub.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Email
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 30)
    @Column(nullable = false, unique = true)
    private String username;

    @Size(max = 15)
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Size(max = 20)
    @Column(name = "national_id", unique = true)
    private String nationalId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE / BANNED

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private Set<RefreshToken> refreshTokens = new HashSet<>();
}
