package com.cinehub.auth.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileRequest {
    
    private UUID userId;
    private String email;
    private String username;
    private String phoneNumber;
    private String nationalId;
}