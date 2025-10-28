package com.cinehub.notification.service;

import com.cinehub.notification.client.UserProfileClient;
import com.cinehub.notification.dto.NotificationResponse;
import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationType;
import com.cinehub.notification.events.BookingTicketGeneratedEvent;
import com.cinehub.notification.events.PaymentSuccessEvent;
import com.cinehub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.MessagingException;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

        private final NotificationRepository notificationRepository;
        private final EmailService emailService;
        private final UserProfileClient userProfileClient;

        // ===========================================================
        // 🎉 Xử lý sự kiện PaymentSuccess
        // ===========================================================
        // @Transactional
        // public void handlePaymentSuccess(PaymentSuccessEvent event) {

        // var profile = userProfileClient.getUserProfile(event.userId().toString());
        // if (profile == null) {
        // log.warn("⚠️ Không tìm thấy profile cho userId {}", event.userId());
        // return;
        // }

        // String userEmail = profile.email();
        // String userName = (profile.fullName() != null &&
        // !profile.fullName().isEmpty())
        // ? profile.fullName()
        // : profile.username();
        // String paymentId = event.paymentId().toString();
        // String bookingId = event.bookingId().toString();
        // String userIdStr = event.userId().toString();
        // double amount = event.amount().doubleValue();
        // String method = event.method();

        // // 🗄️ Lưu Notification vào DB
        // try {
        // String dbMessage = String.format(
        // "Đơn hàng #%s đã thanh toán thành công số tiền %,.0f VNĐ bằng %s.",
        // bookingId.substring(0, 8), amount, method);

        // savePaymentSuccessNotification(event, dbMessage, userEmail, userName);
        // log.info("✅ Lưu thông báo thành công cho userId: {}", event.userId());
        // } catch (Exception dbEx) {
        // log.error("❌ Lỗi khi lưu Notification vào DB cho userId {}: {}",
        // event.userId(),
        // dbEx.getMessage());
        // }

        // // ✉️ Gửi email thông báo
        // try {
        // emailService.sendPaymentSuccessEmail(
        // userEmail, paymentId, bookingId, userIdStr, userName, amount, method);
        // log.info("📧 Gửi email thanh toán thành công cho user: {}", userEmail);
        // } catch (MessagingException e) {
        // log.error("⚠️ Lỗi khi gửi email thanh toán thành công cho userId {}: {}",
        // event.userId(),
        // e.getMessage());
        // }
        // }

        // ===========================================================
        // Tạo Notification khi thanh toán thành công
        // ===========================================================
        // private Notification savePaymentSuccessNotification(
        // PaymentSuccessEvent event,
        // String message,
        // String userEmail,
        // String userName) {

        // String title = "Thanh toán Đơn hàng #" +
        // event.bookingId().toString().substring(0, 8);

        // // Dùng Map.ofEntries để vượt giới hạn 10 phần tử
        // Map<String, Object> metadata = Map.ofEntries(
        // Map.entry("paymentId", event.paymentId()),
        // Map.entry("bookingId", event.bookingId()),
        // Map.entry("showtimeId", event.showtimeId()),
        // Map.entry("userId", event.userId()),
        // Map.entry("userName", userName),
        // Map.entry("userEmail", userEmail),
        // Map.entry("amount", event.amount()),
        // Map.entry("method", event.method()),
        // Map.entry("seatIds", event.seatIds()),
        // Map.entry("eventMessage", event.message()),
        // Map.entry("timestamp", LocalDateTime.now().toString()));

        // Notification notification = Notification.builder()
        // .userId(event.userId())
        // .bookingId(event.bookingId())
        // .paymentId(event.paymentId())
        // .amount(event.amount())
        // .title(title)
        // .message(message)
        // .type(NotificationType.PAYMENT_SUCCESS)
        // .metadata(metadata) // kiểu Map<String,Object>
        // .build();

        // return notificationRepository.save(notification);
        // }

        @Transactional
        public void sendSuccessBookingTicketNotification(BookingTicketGeneratedEvent event) {
                log.info("🎟️ Received BookingTicketGeneratedEvent for bookingId={}", event.bookingId());

                var profile = userProfileClient.getUserProfile(event.userId().toString());
                if (profile == null) {
                        log.warn("⚠️ Không tìm thấy profile cho userId {}", event.userId());
                        return;
                }

                String userEmail = profile.email();
                String userName = (profile.fullName() != null && !profile.fullName().isEmpty())
                                ? profile.fullName()
                                : profile.username();

                try {
                        String title = "🎫 Vé xem phim của bạn đã sẵn sàng!";
                        String message = String.format("""
                                        Bạn đã đặt vé thành công cho phim <b>%s</b> tại rạp <b>%s</b>.<br>
                                        Suất chiếu: <b>%s</b> tại phòng <b>%s</b>.<br>
                                        Tổng tiền: <b>%,.0f VNĐ</b> (%s).<br>
                                        Chúc bạn xem phim vui vẻ! 🎬
                                        """,
                                        event.movieTitle(),
                                        event.cinemaName(),
                                        event.showDateTime(),
                                        event.roomName(),
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
                                        event.movieTitle(),
                                        event.cinemaName(),
                                        event.roomName(),
                                        event.showDateTime(),
                                        event.seats(),
                                        event.fnbs(),
                                        event.promotion(),
                                        event.finalPrice(),
                                        event.paymentMethod());
                        log.info("📧 Gửi email vé xem phim thành công đến {}", userEmail);
                } catch (MessagingException e) {
                        log.error("⚠️ Lỗi khi gửi email vé xem phim cho {}: {}", userEmail, e.getMessage());
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

        // ===========================================================
        // 📋 Lấy danh sách Notification theo user hoặc toàn bộ
        // ===========================================================
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
