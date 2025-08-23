package com.cinehub.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LogOutRequest {
    
    @NotBlank
    private String refreshToken;
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
