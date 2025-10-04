package com.cinehub.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SignInRequest {

    @NotBlank(message = "Username/Email/Phone is required")
    private String usernameOrEmailOrPhone;

    @NotBlank(message = "Password is required")
    private String password;
}
