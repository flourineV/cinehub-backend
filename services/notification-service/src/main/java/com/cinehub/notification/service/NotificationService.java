package com.cinehub.notification.service;

import com.cinehub.notification.dto.NotificationResponse;
import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationStatus;
import com.cinehub.notification.entity.NotificationType;
import com.cinehub.notification.events.PaymentSuccessEvent;
import com.cinehub.notification.events.PaymentFailedEvent;
import com.cinehub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ===================== 🔔 XỬ LÝ EVENT TỪ PAYMENT =====================

    /**
     * 💰 Xử lý khi thanh toán thành công (PaymentSuccessEvent)
     */
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        String message = String.format(
                "Thanh toán thành công %.0f VND cho đơn đặt vé %s qua %s.",
                event.amount(), event.bookingId(), event.method());

        createNotification(
                event.userId(),
                event.bookingId(),
                message,
                NotificationType.PAYMENT_SUCCESS,
                NotificationStatus.SENT);

        log.info("📨 Created PAYMENT_SUCCESS notification for user {}", event.userId());
    }

    /**
     * ❌ Xử lý khi thanh toán thất bại (PaymentFailedEvent)
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        String message = String.format(
                "Thanh toán cho đơn đặt vé %s thất bại. Lý do: %s",
                event.bookingId(), event.reason());

        createNotification(
                event.userId(),
                event.bookingId(),
                message,
                NotificationType.PAYMENT_FAILED,
                NotificationStatus.SENT);

        log.warn("📨 Created PAYMENT_FAILED notification for user {}", event.userId());
    }

    // ===================== 🧩 CRUD & HELPER =====================

    /**
     * ✅ Tạo mới thông báo (cho cả event & admin test)
     */
    @Transactional
    public NotificationResponse createNotification(
            UUID userId,
            UUID bookingId,
            String message,
            NotificationType type,
            NotificationStatus status) {
        Notification notification = Notification.builder()
                .userId(userId)
                .bookingId(bookingId)
                .message(message)
                .type(type)
                .status(status)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("💾 Notification created: {} | user={} | type={}", saved.getId(), userId, type);

        return toResponse(saved);
    }

    /**
     * 📤 Lấy danh sách thông báo theo người dùng
     */
    public List<NotificationResponse> getByUser(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 🗂️ Lấy toàn bộ thông báo (cho admin test)
     */
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 🔁 Cập nhật trạng thái thông báo
     */
    @Transactional
    public void updateStatus(UUID id, NotificationStatus status) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));

        notification.setStatus(status);
        notificationRepository.save(notification);

        log.info("🔄 Notification {} updated to status {}", id, status);
    }

    // ===================== ⚙️ MAPPER =====================
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .bookingId(n.getBookingId())
                .message(n.getMessage())
                .type(n.getType())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
