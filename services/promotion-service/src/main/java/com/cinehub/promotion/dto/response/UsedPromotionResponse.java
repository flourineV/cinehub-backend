package com.cinehub.promotion.dto.response;

import com.cinehub.promotion.entity.UsedPromotion;
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
public class UsedPromotionResponse {
    
    private UUID id;
    private UUID userId;
    private String promotionCode;
    private UUID bookingId;
    private UsedPromotion.BookingStatus bookingStatus;
    private LocalDateTime usedAt;
}
