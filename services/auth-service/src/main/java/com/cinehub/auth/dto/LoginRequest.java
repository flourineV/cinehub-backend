package com.cinehub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Username/Email/Phone is required")
    private String usernameOrEmailOrPhone;
    
    @NotBlank(message = "Password is required")
    private String password;
}
