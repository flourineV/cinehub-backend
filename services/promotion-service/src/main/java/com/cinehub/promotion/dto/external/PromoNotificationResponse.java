package com.cinehub.promotion.dto.external;

import lombok.Data;

@Data
public class PromoNotificationResponse {
    private String message;
    private Integer emailsSent;
    private Integer emailsFailed;
    private String promotionCode;
}
