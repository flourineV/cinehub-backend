package com.cinehub.review.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserProfileResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
}