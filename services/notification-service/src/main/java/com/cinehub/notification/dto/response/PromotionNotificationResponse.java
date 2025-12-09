package com.cinehub.notification.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromotionNotificationResponse {
    private String message;
    private Integer emailsSent;
    private Integer emailsFailed;
    private String promotionCode;
}
