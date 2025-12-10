package com.cinehub.notification.controller;

import com.cinehub.notification.dto.request.FnbOrderConfirmationRequest;
import com.cinehub.notification.dto.response.NotificationResponse;
import com.cinehub.notification.dto.response.PromotionNotificationResponse;
import com.cinehub.notification.dto.request.PromotionNotificationRequest;
import com.cinehub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getAllNotifications() {
        return notificationService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getByUser(@PathVariable UUID userId) {
        return notificationService.getByUser(userId);
    }

    @PostMapping("/promotion")
    public PromotionNotificationResponse createPromotionNotification(
            @RequestBody PromotionNotificationRequest request) {
        return notificationService.createPromotionNotification(request);
    }

    @PostMapping("/fnb-order-confirmation")
    public ResponseEntity<Void> sendFnbOrderConfirmation(@RequestBody FnbOrderConfirmationRequest request) {
        notificationService.sendFnbOrderConfirmationEmail(request);
        return ResponseEntity.ok().build();
    }
}
