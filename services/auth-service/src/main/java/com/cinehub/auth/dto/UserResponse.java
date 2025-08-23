package com.cinehub.auth.dto;

import com.cinehub.auth.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {
    
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String favoriteGenres;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public UserResponse() {}
    
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatarUrl = user.getAvatarUrl();
        this.gender = user.getGender();
        this.dateOfBirth = user.getDateOfBirth();
        this.favoriteGenres = user.getFavoriteGenres();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getFavoriteGenres() {
        return favoriteGenres;
    }
    
    public void setFavoriteGenres(String favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }
    
    public User.Role getRole() {
        return role;
    }
    
    public void setRole(User.Role role) {
        this.role = role;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
