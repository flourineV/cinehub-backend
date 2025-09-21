package com.cinehub.profile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    
    @Column(name = "user_id", nullable = false)
    private UUID userId; // Reference to User ID from auth service
    
    @Email
    @NotBlank
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String email;
    
    @Size(max = 50)
    @Column(name = "username", unique = true)
    private String username;
    
    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Size(max = 20)
    @Column(name = "national_id")
    private String nationalId;
    
    @Size(max = 100)
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Size(max = 10)
    @Column(name = "gender")
    private String gender;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_genres", columnDefinition = "TEXT")
    private List<String> favoriteGenres;
          
    @Column(name = "loyalty_point")
    @Builder.Default
    private Integer loyaltyPoint = 0;
    
    @Size(max = 20)
    @Column(name = "rank")
    private String rank;
    
    @Size(max = 20)
    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}