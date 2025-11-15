package com.cinehub.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {

    private UUID id;
    private UUID userId;
    private String activityType;
    private String entityType;
    private UUID entityId;
    private String metadata;
    private LocalDateTime createdAt;
}
