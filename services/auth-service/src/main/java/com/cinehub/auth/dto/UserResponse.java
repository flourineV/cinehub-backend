package com.cinehub.auth.dto;

import com.cinehub.auth.entity.User;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private UUID id;
    private String username;
    private User.Role role;

    // Constructor nhận User entity
    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole();
    }
}
