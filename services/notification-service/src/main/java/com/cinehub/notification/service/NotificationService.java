package com.cinehub.notification.service;

import com.cinehub.notification.client.UserProfileClient;
import com.cinehub.notification.dto.request.FnbOrderConfirmationRequest;
import com.cinehub.notification.dto.response.NotificationResponse;
import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationType;
import com.cinehub.notification.events.BookingTicketGeneratedEvent;
import com.cinehub.notification.events.BookingRefundedEvent;

import com.cinehub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.MessagingException;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

        private final NotificationRepository notificationRepository;
        private final EmailService emailService;
        private final UserProfileClient userProfileClient;
        private final MessageTemplateService messageTemplateService;

        @Transactional
        public void sendBookingRefundProcessedNotification(BookingRefundedEvent event) {
                log.info("Processing BookingRefundedEvent for bookingId={}", event.bookingId());

                String userEmail;
                String userName;
                String language = event.language() != null ? event.language() : "vi"; // Lấy từ event

                if (event.userId() != null) {
                        // Case: User đã đăng ký -> Gọi Profile Service
                        try {
                                var profile = userProfileClient.getUserProfile(event.userId().toString());
                                if (profile == null) {
                                        log.warn("Không tìm thấy profile cho userId {}", event.userId());
                                        return;
                                }
                                userEmail = profile.email();
                                userName = (profile.fullName() != null && !profile.fullName().isEmpty())
                                                ? profile.fullName()
                                                : profile.username();
                        } catch (Exception e) {
                                log.error("Lỗi khi fetch user profile: {}", e.getMessage());
                                return;
                        }
                } else {
                        // Case: Guest -> Lấy trực tiếp từ Event
                        userEmail = event.guestEmail();
                        userName = event.guestName();
                }

                if (userEmail == null || userEmail.isEmpty()) {
                        log.warn("Không có email để gửi notification hoàn tiền cho booking {}", event.bookingId());
                        return;
                }
                if (event.userId() != null) {
                        String title = messageTemplateService.getRefundTitle(language);
                        String message;
                        if ("VOUCHER".equalsIgnoreCase(event.refundMethod())) {
                                message = messageTemplateService.getRefundVoucherMessage(
                                                language, event.bookingId(), event.refundedValue(), event.reason());
                        } else {
                                message = messageTemplateService.getRefundCashMessage(
                                                language, event.bookingId(), event.reason());
                        }

                        createNotification(
                                        event.userId(),
                                        event.bookingId(),
                                        null,
                                        event.refundedValue(),
                                        title,
                                        message,
                                        NotificationType.BOOKING_REFUNDED,
                                        language,
                                        Map.of("reason", event.reason(), "method", event.refundMethod()));
                }

                // 3. Gửi Email (Cho cả User và Guest)
                try {
                        emailService.sendRefundEmail(
                                        userEmail,
                                        userName,
                                        event.bookingId(),
                                        event.refundedValue(),
                                        event.refundMethod(),
                                        event.reason(),
                                        language);
                } catch (MessagingException e) {
                        log.error("Lỗi khi gửi email hoàn tiền cho {}: {}", userEmail, e.getMessage());
                }
        }

        @Transactional
        public void sendSuccessBookingTicketNotification(BookingTicketGeneratedEvent event) {
                log.info("Received BookingTicketGeneratedEvent for bookingId={}", event.bookingId());

                String userEmail;
                String userName;
                String language = event.language() != null ? event.language() : "vi"; // Lấy từ event

                if (event.userId() != null) {
                        // Case: User đã đăng ký -> Gọi Profile Service
                        try {
                                var profile = userProfileClient.getUserProfile(event.userId().toString());
                                if (profile == null) {
                                        log.warn("Không tìm thấy profile cho userId {}", event.userId());
                                        return;
                                }
                                userEmail = profile.email();
                                userName = (profile.fullName() != null && !profile.fullName().isEmpty())
                                                ? profile.fullName()
                                                : profile.username();
                        } catch (Exception e) {
                                log.error("Lỗi khi fetch user profile: {}", e.getMessage());
                                return;
                        }
                } else {
                        // Case: Guest -> Lấy trực tiếp từ Event
                        userEmail = event.guestEmail();
                        userName = event.guestName();
                }

                // Chỉ lưu notification vào DB khi có userId (user đăng nhập)
                // Guest không có userId nên không lưu notification, chỉ gửi email
                if (event.userId() != null) {
                        try {
                                String title = messageTemplateService.getBookingTicketTitle(language);
                                String message = messageTemplateService.getBookingTicketMessage(
                                                language,
                                                event.movieTitle(),
                                                event.cinemaName(),
                                                event.showDateTime(),
                                                event.roomName(),
                                                event.totalPrice(),
                                                event.rankName(),
                                                event.rankDiscountAmount(),
                                                event.promotion() != null ? event.promotion().code() : null,
                                                event.promotion() != null ? event.promotion().discountAmount()
                                                                : BigDecimal.ZERO,
                                                event.finalPrice(),
                                                event.paymentMethod());

                                Map<String, Object> metadata = Map.ofEntries(
                                                Map.entry("bookingId", event.bookingId().toString()),
                                                Map.entry("bookingCode", event.bookingCode()),
                                                Map.entry("userId", event.userId().toString()),
                                                Map.entry("movieTitle", event.movieTitle()),
                                                Map.entry("cinemaName", event.cinemaName()),
                                                Map.entry("roomName", event.roomName()),
                                                Map.entry("showDateTime", event.showDateTime().toString()),
                                                Map.entry("seats", event.seats() != null ? event.seats().toString() : ""),
                                                Map.entry("fnbs", event.fnbs() != null ? event.fnbs().toString() : ""),
                                                Map.entry("promotionCode",
                                                                event.promotion() != null ? event.promotion().code() : ""),
                                                Map.entry("rankName", event.rankName()),
                                                Map.entry("rankDiscountAmount", event.rankDiscountAmount().toString()),
                                                Map.entry("totalPrice", event.totalPrice().toString()),
                                                Map.entry("finalPrice", event.finalPrice().toString()),
                                                Map.entry("paymentMethod", event.paymentMethod()),
                                                Map.entry("createdAt", event.createdAt().toString()));

                                Notification notification = Notification.builder()
                                                .userId(event.userId())
                                                .title(title)
                                                .message(message)
                                                .language(language)
                                                .type(NotificationType.BOOKING_TICKET)
                                                .metadata(metadata)
                                                .build();

                                notificationRepository.save(notification);
                                log.info("Notification (BOOKING_TICKET) saved for user {}", userEmail);
                        } catch (Exception e) {
                                log.error("Lỗi khi lưu notification vé xem phim: ", e);
                        }
                } else {
                        log.info("Guest booking - skipping notification save, will only send email to {}", userEmail);
                }

                // Gửi email cho cả User và Guest
                try {
                        emailService.sendBookingTicketEmail(
                                        userEmail,
                                        userName,
                                        event.bookingId(),
                                        event.bookingCode(),
                                        event.movieTitle(),
                                        event.cinemaName(),
                                        event.roomName(),
                                        event.showDateTime(),
                                        event.seats(),
                                        event.fnbs(),
                                        event.promotion(),
                                        event.rankName(),
                                        event.rankDiscountAmount(),
                                        event.totalPrice(),
                                        event.finalPrice(),
                                        event.paymentMethod(),
                                        language);
                        log.info("Gửi email vé xem phim thành công đến {} (language: {})", userEmail, language);
                } catch (MessagingException e) {
                        log.error("Lỗi khi gửi email vé xem phim cho {}: {}", userEmail, e.getMessage());
                }
        }

        @Transactional
        public Notification createNotification(
                        UUID userId,
                        UUID bookingId,
                        UUID paymentId,
                        BigDecimal amount,
                        String title,
                        String message,
                        NotificationType type,
                        String language,
                        Map<String, Object> metadata) {

                if (userId == null || type == null) {
                        throw new IllegalArgumentException("userId và type không được null");
                }

                Notification notification = Notification.builder()
                                .userId(userId)
                                .title(title != null ? title
                                                : messageTemplateService
                                                                .getDefaultTitle(language != null ? language : "vi"))
                                .message(message)
                                .type(type)
                                .language(language != null ? language : "vi")
                                .metadata(metadata)
                                .build();

                Notification saved = notificationRepository.save(notification);
                log.info("[Notification] Created new {} for userId={} with title='{}'",
                                type, userId, saved.getTitle());
                return saved;
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
                                .message(n.getMessage())
                                .language(n.getLanguage())
                                .type(n.getType())
                                .createdAt(n.getCreatedAt())
                                .build();
        }

        @Transactional
        public com.cinehub.notification.dto.response.PromotionNotificationResponse createPromotionNotification(
                        com.cinehub.notification.dto.request.PromotionNotificationRequest request) {
                log.info("Sending promotion notification for code: {}", request.getPromotionCode());

                // Fetch subscribed users emails from user-profile-service
                List<String> subscribedEmails = userProfileClient.getSubscribedUsersEmails();
                log.info("Found {} subscribed users for promotion notification", subscribedEmails.size());

                int emailsSent = 0;
                int emailsFailed = 0;

                for (String email : subscribedEmails) {
                        try {
                                emailService.sendPromotionEmail(
                                                email,
                                                request.getPromotionCode(),
                                                request.getDiscountType(),
                                                request.getDiscountValue(),
                                                request.getDiscountValueDisplay(),
                                                request.getDescription(),
                                                request.getPromoDisplayUrl(),
                                                request.getStartDate(),
                                                request.getEndDate(),
                                                request.getUsageRestriction(),
                                                request.getActionUrl(),
                                                "FIRST_TIME".equals(request.getPromotionType()));
                                emailsSent++;
                        } catch (MessagingException e) {
                                log.error("Failed to send promotion email to {}: {}", email, e.getMessage());
                                emailsFailed++;
                        }
                }

                return com.cinehub.notification.dto.response.PromotionNotificationResponse.builder()
                                .message("Promotion notification sent")
                                .emailsSent(emailsSent)
                                .emailsFailed(emailsFailed)
                                .promotionCode(request.getPromotionCode())
                                .build();
        }

        public void sendFnbOrderConfirmationEmail(FnbOrderConfirmationRequest request) {
                log.info("Sending FnB order confirmation email for orderCode: {}", request.getOrderCode());

                String language = request.getLanguage() != null ? request.getLanguage() : "vi";

                try {
                        emailService.sendFnbOrderConfirmationEmail(
                                        request.getUserEmail(),
                                        request.getUserName(),
                                        request.getOrderCode(),
                                        request.getTotalAmount(),
                                        request.getItems(),
                                        language);
                        log.info("✅ FnB order confirmation email sent to: {} (language: {})", request.getUserEmail(), language);
                } catch (MessagingException e) {
                        log.error("❌ Failed to send FnB order confirmation email: {}", e.getMessage(), e);
                }
        }

        @Transactional
        public void sendFnbOrderConfirmationEmailFromEvent(
                        com.cinehub.notification.events.FnbOrderConfirmedEvent event) {
                log.info("Processing FnbOrderConfirmedEvent for orderCode: {}", event.orderCode());

                String userEmail = "user@example.com";
                String userName = "Customer";
                String language = event.language() != null ? event.language() : "vi";

                // Fetch user profile
                try {
                        var profile = userProfileClient.getUserProfile(event.userId().toString());
                        if (profile != null) {
                                userEmail = profile.email();
                                userName = (profile.fullName() != null && !profile.fullName().isEmpty())
                                                ? profile.fullName()
                                                : profile.username();
                        } else {
                                log.warn("User profile not found for userId: {}, using default values",
                                                event.userId());
                        }
                } catch (Exception e) {
                        log.error("Failed to fetch user profile for userId {}: {}", event.userId(), e.getMessage());
                }

                // Convert event items to EmailService format
                List<com.cinehub.notification.dto.request.FnbOrderConfirmationRequest.FnbItemDetail> emailItems = event
                                .items()
                                .stream()
                                .map(item -> new com.cinehub.notification.dto.request.FnbOrderConfirmationRequest.FnbItemDetail(
                                                item.itemName(),
                                                item.itemNameEn(),
                                                item.quantity(),
                                                item.unitPrice(),
                                                item.totalPrice()))
                                .toList();

                // Save notification to DB
                try {
                        String title = messageTemplateService.getFnbOrderTitle(language);
                        String message = messageTemplateService.getFnbOrderMessage(
                                        language, event.orderCode(), event.totalAmount());

                        List<Map<String, Object>> itemsMetadata = event.items().stream()
                                        .map(item -> Map.<String, Object>of(
                                                        "itemName", item.itemName(),
                                                        "itemNameEn", item.itemNameEn() != null ? item.itemNameEn() : "",
                                                        "quantity", item.quantity(),
                                                        "unitPrice", item.unitPrice().toString(),
                                                        "totalPrice", item.totalPrice().toString()))
                                        .toList();

                        Map<String, Object> metadata = Map.of(
                                        "orderCode", event.orderCode(),
                                        "orderId", event.orderId().toString(),
                                        "totalAmount", event.totalAmount().toString(),
                                        "items", itemsMetadata);

                        Notification notification = Notification.builder()
                                        .userId(event.userId())
                                        .title(title)
                                        .message(message)
                                        .language(language)
                                        .type(NotificationType.FNB_ORDER)
                                        .metadata(metadata)
                                        .build();

                        notificationRepository.save(notification);
                        log.info("Notification (FNB_ORDER) saved for user {}", event.userId());
                } catch (Exception e) {
                        log.error("Lỗi khi lưu notification FnB order: ", e);
                }

                // Send email
                try {
                        emailService.sendFnbOrderConfirmationEmail(
                                        userEmail,
                                        userName,
                                        event.orderCode(),
                                        event.totalAmount(),
                                        emailItems,
                                        language);
                        log.info("✅ FnB order confirmation email sent to: {} (language: {})", userEmail, language);
                } catch (MessagingException e) {
                        log.error("❌ Failed to send FnB order confirmation email: {}", e.getMessage(), e);
                }
        }
}
