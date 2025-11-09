package com.cinehub.notification.service;

import com.cinehub.notification.client.UserProfileClient;
import com.cinehub.notification.dto.NotificationResponse;
import com.cinehub.notification.entity.Notification;
import com.cinehub.notification.entity.NotificationType;
import com.cinehub.notification.events.BookingTicketGeneratedEvent;

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
        public void sendSuccessBookingTicketNotification(BookingTicketGeneratedEvent event) {
                log.info("Received BookingTicketGeneratedEvent for bookingId={}", event.bookingId());

                var profile = userProfileClient.getUserProfile(event.userId().toString());
                if (profile == null) {
                        log.warn("Không tìm thấy profile cho userId {}", event.userId());
                        return;
                }

                String userEmail = profile.email();
                String userName = (profile.fullName() != null && !profile.fullName().isEmpty())
                                ? profile.fullName()
                                : profile.username();

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
                        event.promotion() != null ? event.promotion().discountAmount() : BigDecimal.ZERO,
                        event.finalPrice(),
                        event.paymentMethod()
                        );


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
                        Map.entry("createdAt", event.createdAt().toString())
                        );

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
}
