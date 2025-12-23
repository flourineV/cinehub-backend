package com.cinehub.notification.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MessageTemplateService {

    // ========== BOOKING TICKET TEMPLATES ==========

    public String getBookingTicketTitle(String language) {
        return "en".equalsIgnoreCase(language)
                ? "Your movie ticket is ready!"
                : "Vé xem phim của bạn đã sẵn sàng!";
    }

    public String getBookingTicketMessage(
            String language,
            String movieTitle,
            String cinemaName,
            String showDateTime,
            String roomName,
            BigDecimal totalPrice,
            String rankName,
            BigDecimal rankDiscountAmount,
            String promotionCode,
            BigDecimal promotionDiscountAmount,
            BigDecimal finalPrice,
            String paymentMethod) {

        if ("en".equalsIgnoreCase(language)) {
            return String.format("""
                    You have successfully booked tickets for <b>%s</b> at <b>%s</b> cinema.<br>
                    Showtime: <b>%s</b> in room <b>%s</b>.<br><br>
                    <b>Invoice details:</b><br>
                    - Original price: <b>%,.0f VND</b><br>
                    - %s discount: <b>-%,.0f VND</b><br>
                    - Promotion discount (%s): <b>-%,.0f VND</b><br>
                    -------------------------------------------<br>
                    <b>Total: %,.0f VND</b> (%s).<br><br>
                    Enjoy your movie!
                    """,
                    movieTitle,
                    cinemaName,
                    showDateTime,
                    roomName,
                    totalPrice,
                    rankName,
                    rankDiscountAmount,
                    promotionCode != null && !promotionCode.isEmpty() ? promotionCode : "None",
                    promotionDiscountAmount,
                    finalPrice,
                    paymentMethod);
        } else {
            return String.format("""
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
                    movieTitle,
                    cinemaName,
                    showDateTime,
                    roomName,
                    totalPrice,
                    rankName,
                    rankDiscountAmount,
                    promotionCode != null && !promotionCode.isEmpty() ? promotionCode : "Không có",
                    promotionDiscountAmount,
                    finalPrice,
                    paymentMethod);
        }
    }

    // ========== REFUND TEMPLATES ==========

    public String getRefundTitle(String language) {
        return "en".equalsIgnoreCase(language)
                ? "Refund / Ticket Cancellation Notice"
                : "Thông báo hoàn tiền / Hủy vé";
    }

    public String getRefundVoucherMessage(
            String language,
            UUID bookingId,
            BigDecimal refundedValue,
            String reason) {

        if ("en".equalsIgnoreCase(language)) {
            return String.format(
                    "Your ticket for order %s has been successfully refunded as a Voucher worth %,.0f VND. Reason: %s",
                    bookingId, refundedValue, reason);
        } else {
            return String.format(
                    "Vé cho đơn hàng %s đã được hoàn tiền thành công dưới dạng Voucher trị giá %,.0f VNĐ. Lý do: %s",
                    bookingId, refundedValue, reason);
        }
    }

    public String getRefundCashMessage(
            String language,
            UUID bookingId,
            String reason) {

        if ("en".equalsIgnoreCase(language)) {
            return String.format(
                    "Your ticket for order %s has been cancelled. Please contact the ticket counter for refund. Reason: %s",
                    bookingId, reason);
        } else {
            return String.format(
                    "Vé cho đơn hàng %s đã bị hủy. Vui lòng liên hệ quầy vé để nhận hoàn tiền. Lý do: %s",
                    bookingId, reason);
        }
    }

    // ========== DEFAULT TEMPLATES ==========

    public String getDefaultTitle(String language) {
        return "en".equalsIgnoreCase(language)
                ? "Notification from CineHub"
                : "Thông báo từ CineHub";
    }

    // ========== FNB ORDER TEMPLATES ==========

    public String getFnbOrderTitle(String language) {
        return "en".equalsIgnoreCase(language)
                ? "Your F&B order is confirmed!"
                : "Đơn hàng bắp nước đã được xác nhận!";
    }

    public String getFnbOrderMessage(String language, String orderCode, BigDecimal totalAmount) {
        if ("en".equalsIgnoreCase(language)) {
            return String.format(
                    "Your F&B order <b>%s</b> has been confirmed.<br>Total: <b>%,.0f VND</b>.<br>Please pick up your order at the counter.",
                    orderCode, totalAmount);
        } else {
            return String.format(
                    "Đơn hàng bắp nước <b>%s</b> của bạn đã được xác nhận.<br>Tổng tiền: <b>%,.0f VNĐ</b>.<br>Vui lòng nhận hàng tại quầy.",
                    orderCode, totalAmount);
        }
    }
}
