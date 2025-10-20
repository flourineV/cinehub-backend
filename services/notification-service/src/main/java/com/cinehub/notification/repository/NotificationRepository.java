package com.cinehub.notification.repository;

import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByType(NotificationType type);
}
