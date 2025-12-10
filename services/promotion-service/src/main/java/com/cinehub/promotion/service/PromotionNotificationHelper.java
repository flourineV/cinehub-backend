package com.cinehub.promotion.service;

import com.cinehub.promotion.config.NotificationClient;
import com.cinehub.promotion.entity.Promotion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionNotificationHelper {

    private final NotificationClient notificationClient;

    public void sendPromotionNotification(Promotion promotion) {
        // Format discount display
        String discountDisplay = promotion.getDiscountType().name().equals("PERCENTAGE")
                ? promotion.getDiscountValue() + "%"
                : String.format("%,.0f VNĐ", promotion.getDiscountValue());

        // Format usage restriction
        String usageRestriction = buildUsageRestriction(promotion);

        // Format valid until
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        String validUntil = promotion.getEndDate().format(formatter);

        // Build notification request
        com.cinehub.promotion.dto.request.PromotionNotificationRequest notifRequest = com.cinehub.promotion.dto.request.PromotionNotificationRequest
                .builder()
                .promotionCode(promotion.getCode())
                .promotionType(promotion.getPromotionType().name())
                .discountType(promotion.getDiscountType().name())
                .discountValue(promotion.getDiscountValue())
                .discountValueDisplay(discountDisplay)
                .description(promotion.getDescription())
                .promoDisplayUrl(promotion.getPromoDisplayUrl())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .validUntil(validUntil)
                .usageRestriction(usageRestriction)
                .actionUrl("https://cinehub.com/movies")
                .build();

        notificationClient.sendPromotionNotification(notifRequest);
        log.info("Sent promotion notification for code: {}", promotion.getCode());
    }

    private String buildUsageRestriction(Promotion promotion) {
        if (promotion.getUsageTimeRestriction() == null ||
                promotion.getUsageTimeRestriction() == Promotion.UsageTimeRestriction.NONE) {
            return "Áp dụng mọi lúc";
        }

        switch (promotion.getUsageTimeRestriction()) {
            case WEEKEND_ONLY:
                return "Chỉ áp dụng cuối tuần";
            case WEEKDAY_ONLY:
                return "Chỉ áp dụng ngày trong tuần";
            case CUSTOM_DAYS:
                StringBuilder sb = new StringBuilder();
                if (promotion.getAllowedDaysOfWeek() != null && !promotion.getAllowedDaysOfWeek().isEmpty()) {
                    sb.append("Áp dụng vào: ").append(promotion.getAllowedDaysOfWeek());
                }
                if (promotion.getAllowedDaysOfMonth() != null && !promotion.getAllowedDaysOfMonth().isEmpty()) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append("Ngày ").append(promotion.getAllowedDaysOfMonth()).append(" trong tháng");
                }
                return sb.length() > 0 ? sb.toString() : "Áp dụng theo lịch tùy chỉnh";
            default:
                return "Áp dụng mọi lúc";
        }
    }
}
