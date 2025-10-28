package com.cinehub.notification.controller;

import com.cinehub.notification.dto.NotificationResponse;
import com.cinehub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
}
