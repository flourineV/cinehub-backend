package com.cinehub.notification.service;

import com.cinehub.notification.client.UserProfileClient;
import com.cinehub.notification.dto.NotificationResponse;
import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationType;
import com.cinehub.notification.events.PaymentSuccessEvent;
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
        private final EmailService emailService;
        private final UserProfileClient userProfileClient;

        @Transactional
        public void handlePaymentSuccess(PaymentSuccessEvent event) {
                String message = String.format(
                                "🎉 Thanh toán thành công %.0f VND cho đơn đặt vé %s qua %s.",
                                event.amount(), event.bookingId(), event.method());

                var profile = userProfileClient.getUserProfile(event.userId().toString());
                if (profile == null) {
                        log.warn("⚠️ Không tìm thấy profile cho userId {}, chỉ lưu notification.", event.userId());
                        createNotification(event.userId(), event.bookingId(), message,
                                        NotificationType.PAYMENT_SUCCESS);
                        return;
                }

                String email = profile.email();
                String name = (profile.fullName() != null && !profile.fullName().isEmpty())
                                ? profile.fullName()
                                : profile.username();

                // 💾 Lưu notification
                Notification notification = createNotification(
                                event.userId(),
                                event.bookingId(),
                                message,
                                NotificationType.PAYMENT_SUCCESS);

                // 📧 Gửi mail nếu có email
                if (email != null && !email.isEmpty()) {
                        String personalizedMsg = "Xin chào " + (name != null ? name : "bạn") + "!\n" + message;
                        emailService.sendEmail(email, "CineHub - Thanh toán thành công 🎬", personalizedMsg);
                        log.info("📧 Email sent to {} for booking {}", email, event.bookingId());
                } else {
                        log.warn("⚠️ User {} không có email, bỏ qua gửi mail.", event.userId());
                }

                log.info("✅ PAYMENT_SUCCESS notification stored for user {}", event.userId());
        }

        // =====================================================
        // 🎯 Gửi khuyến mãi (Admin gọi API)
        // =====================================================
        @Transactional
        public NotificationResponse sendPromotion(UUID userId, String title, String message) {
                var profile = userProfileClient.getUserProfile(userId.toString());
                String email = (profile != null) ? profile.email() : null;

                Notification notification = createNotification(userId, null, message, NotificationType.PROMOTION);

                if (email != null && !email.isEmpty()) {
                        emailService.sendEmail(
                                        email,
                                        title != null ? title : "🎁 Ưu đãi đặc biệt từ CineHub!",
                                        message);
                        log.info("🎁 Promotion email sent to {}", email);
                } else {
                        log.warn("⚠️ Không tìm thấy email cho userId {}, bỏ qua gửi mail.", userId);
                }

                return toResponse(notification);
        }

        // =====================================================
        // ⚙️ Helper & CRUD
        // =====================================================
        @Transactional
        public Notification createNotification(UUID userId, UUID bookingId, String message, NotificationType type) {
                Notification notification = Notification.builder()
                                .userId(userId)
                                .bookingId(bookingId)
                                .message(message)
                                .type(type)
                                .build();
                return notificationRepository.save(notification);
        }

        public List<NotificationResponse> getByUser(UUID userId) {
                return notificationRepository.findByUserId(userId).stream()
                                .map(this::toResponse)
                                .toList();
        }

        public List<NotificationResponse> getAll() {
                return notificationRepository.findAll().stream()
                                .map(this::toResponse)
                                .toList();
        }

        private NotificationResponse toResponse(Notification n) {
                return NotificationResponse.builder()
                                .id(n.getId())
                                .userId(n.getUserId())
                                .bookingId(n.getBookingId())
                                .message(n.getMessage())
                                .type(n.getType())
                                .createdAt(n.getCreatedAt())
                                .build();
        }
}
