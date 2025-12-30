package com.cinehub.showtime.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.cinehub.showtime.dto.request.ProvinceRequest;
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    private static final String NOTIFICATION_SERVICE_URL =
            "http://notification-service:8085/api/notifications";

    @Value("${app.internal.secret-key}")
    private String internalSecretKey;

    /**
     * Send notification to user
     */
    public void sendNotification(ProvinceRequest request) {
        try {
            String url = NOTIFICATION_SERVICE_URL + "/send";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecretKey);

            HttpEntity<ProvinceRequest> requestEntity =
                    new HttpEntity<>(request, headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );

            log.info(
                "Notification sent successfully. userId={}, type={}",
                request.getName(),
                request.getNameEn()
            );
        } catch (Exception e) {
            log.error(
                "Failed to send notification. userId={}, type={}",
                request.getName(),
                request.getNameEn(),
                e
            );
        }
    }
}
