package com.cinehub.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;
    
    private LocalDate dateOfBirth;
    
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
    
    @Size(max = 10, message = "Gender must be less than 10 characters")
    private String gender;
    
    private String avatarUrl;
    
    private List<String> favoriteGenres;
    
    @Size(max = 20, message = "Rank must be less than 20 characters")
    private String rank;
    
    @Size(max = 20, message = "Status must be less than 20 characters")
    private String status;
}