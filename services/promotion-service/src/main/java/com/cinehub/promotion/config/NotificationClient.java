package com.cinehub.promotion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.cinehub.promotion.dto.external.PromoNotificationResponse;
import com.cinehub.promotion.dto.request.PromotionNotificationRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final WebClient notificationWebClient;

    public PromoNotificationResponse sendPromotionNotification(PromotionNotificationRequest request) {
        try {
            return notificationWebClient.post()
                    .uri("/api/notifications/promotion")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PromoNotificationResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to send promotion notification: {}", e.getMessage());
            return null;
        }
    }
}
