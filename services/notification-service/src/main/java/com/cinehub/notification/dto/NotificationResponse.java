package com.cinehub.notification.dto;

import com.cinehub.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private UUID bookingId;
    private UUID paymentId;
    private Double amount;

    private String title;
    private String message;
    private NotificationType type;

    // Metadata được deserialize thành chuỗi JSON (FE có thể parse lại nếu cần)
    private String metadata;

    private LocalDateTime createdAt;
}
