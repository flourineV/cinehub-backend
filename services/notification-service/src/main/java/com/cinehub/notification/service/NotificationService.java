package com.cinehub.notification.service;

import com.cinehub.notification.client.UserProfileClient;
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

        @Transactional
        public void sendBookingRefundProcessedNotification(BookingRefundedEvent event) {
                log.info("Processing BookingRefundedEvent for bookingId={}", event.bookingId());

                String userEmail;
                String userName;
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
                        String message;
                        if ("VOUCHER".equalsIgnoreCase(event.refundMethod())) {
                                message = String.format(
                                                "Vé cho đơn hàng %s đã được hoàn tiền thành công dưới dạng Voucher trị giá %,.0f VNĐ. Lý do: %s",
                                                event.bookingId(), event.refundedValue(), event.reason());
                        } else {
                                message = String.format(
                                                "Vé cho đơn hàng %s đã bị hủy. Vui lòng liên hệ quầy vé để nhận hoàn tiền. Lý do: %s",
                                                event.bookingId(), event.reason());
                        }

                        createNotification(
                                        event.userId(),
                                        event.bookingId(),
                                        null,
                                        event.refundedValue(),
                                        "Thông báo hoàn tiền / Hủy vé",
                                        message,
                                        NotificationType.BOOKING_REFUNDED, // Hoặc tạo type BOOKING_REFUNDED
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
                                        event.reason());
                } catch (MessagingException e) {
                        log.error("Lỗi khi gửi email hoàn tiền cho {}: {}", userEmail, e.getMessage());
                }
        }

        @Transactional
        public void sendSuccessBookingTicketNotification(BookingTicketGeneratedEvent event) {
                log.info("Received BookingTicketGeneratedEvent for bookingId={}", event.bookingId());

                String userEmail;
                String userName;
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

                try {
                        String title = "Vé xem phim của bạn đã sẵn sàng!";
                        String message = String.format("""
                                        Bạn đã đặt vé thành công cho phim <b>%s</b> tại rạp <b>%s</b>.<br>
                                        Suất chiếu: <b>%s</b> tại phòng <b>%s</b>.<br><br>
                                        <b>Chi tiết hóa đơn:</b><br>
                                        - Tổng giá gốc: <b>%,.0f VNĐ</b><br>
                                        - Giảm giá hạng %s: <b>-%,.0f VNĐ</b><br>
                                        - Giảm giá khuyến mãi (%s): <b>-%,.0f VNĐ</b><br>
                                        -------------------------------------------<br>
                                        <b>Thành tiền: %,.0f VNĐ</b> (%s).<br><br>
                                        Chúc bạn xem phim vui vẻ!
                                        """,
                                        event.movieTitle(),
                                        event.cinemaName(),
                                        event.showDateTime(),
                                        event.roomName(),
                                        event.totalPrice(),
                                        event.rankName(),
                                        event.rankDiscountAmount(),
                                        event.promotion() != null ? event.promotion().code() : "Không có",
                                        event.promotion() != null ? event.promotion().discountAmount()
                                                        : BigDecimal.ZERO,
                                        event.finalPrice(),
                                        event.paymentMethod());

                        Map<String, Object> metadata = Map.ofEntries(
                                        Map.entry("bookingId", event.bookingId()),
                                        Map.entry("userId", event.userId()),
                                        Map.entry("movieTitle", event.movieTitle()),
                                        Map.entry("cinemaName", event.cinemaName()),
                                        Map.entry("roomName", event.roomName()),
                                        Map.entry("showDateTime", event.showDateTime()),
                                        Map.entry("seats", event.seats()),
                                        Map.entry("fnbs", event.fnbs()),
                                        Map.entry("promotion", event.promotion()),
                                        Map.entry("rankName", event.rankName()),
                                        Map.entry("rankDiscountAmount", event.rankDiscountAmount()),
                                        Map.entry("totalPrice", event.totalPrice()),
                                        Map.entry("finalPrice", event.finalPrice()),
                                        Map.entry("paymentMethod", event.paymentMethod()),
                                        Map.entry("createdAt", event.createdAt().toString()));

                        Notification notification = Notification.builder()
                                        .userId(event.userId())
                                        .bookingId(event.bookingId())
                                        .title(title)
                                        .message(message)
                                        .type(NotificationType.BOOKING_TICKET)
                                        .metadata(metadata)
                                        .build();

                        notificationRepository.save(notification);
                        log.info("Notification (BOOKING_TICKET) saved for user {}", userEmail);
                } catch (Exception e) {
                        log.error("Lỗi khi lưu notification vé xem phim: {}", e.getMessage());
                }

                try {
                        emailService.sendBookingTicketEmail(
                                        userEmail,
                                        userName,
                                        event.bookingId(),
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
                                        event.paymentMethod());
                        log.info("Gửi email vé xem phim thành công đến {}", userEmail);
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
                        Map<String, Object> metadata) {

                if (userId == null || type == null) {
                        throw new IllegalArgumentException("userId và type không được null");
                }

                Notification notification = Notification.builder()
                                .userId(userId)
                                .bookingId(bookingId)
                                .paymentId(paymentId)
                                .amount(amount)
                                .title(title != null ? title : "Thông báo từ CineHub")
                                .message(message)
                                .type(type)
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
                                .bookingId(n.getBookingId())
                                .message(n.getMessage())
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
}
