package com.cinehub.auth.dto;

import com.cinehub.auth.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private UUID id;
    private String email;
    private String fullName;
    private LocalDate dateOfBirth;
    private String username;
    private String phoneNumber;
    private String nationalId;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor nhận User entity
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.dateOfBirth = user.getDateOfBirth();
        this.username = user.getUsername();
        this.phoneNumber = user.getPhoneNumber();
        this.nationalId = user.getNationalId();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
