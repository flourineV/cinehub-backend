package com.cinehub.promotion.dto.request;

import com.cinehub.promotion.entity.UsedPromotion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePromotionUsageStatusRequest {
    
    private UUID bookingId;
    private UsedPromotion.BookingStatus bookingStatus;
}
