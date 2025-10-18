package com.cinehub.auth.dto.response;

import com.cinehub.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String role;
    private String status;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.status = user.getStatus();
        this.role = user.getRole() != null ? user.getRole().getName() : null;
    }
}
