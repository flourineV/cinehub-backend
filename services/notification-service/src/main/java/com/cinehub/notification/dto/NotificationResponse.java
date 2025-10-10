package com.cinehub.notification.dto;

import com.cinehub.notification.entity.NotificationStatus;
import com.cinehub.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private UUID bookingId;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
