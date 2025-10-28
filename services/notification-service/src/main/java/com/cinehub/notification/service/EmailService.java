package com.cinehub.notification.service;

import com.cinehub.notification.dto.external.FnbDetail;
import com.cinehub.notification.dto.external.PromotionDetail;
import com.cinehub.notification.dto.external.SeatDetail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine templateEngine;

    /**
     * Gửi email “Vé xem phim thành công” (sau khi Booking phát
     * BookingTicketGeneratedEvent).
     * Template gợi ý: resources/templates/booking-ticket.html
     */
    public void sendBookingTicketEmail(
            String to,
            String userName,
            String movieTitle,
            String cinemaName,
            String roomName,
            String showDateTime,
            List<SeatDetail> seats,
            List<FnbDetail> fnbs,
            PromotionDetail promotion,
            BigDecimal finalPrice,
            String paymentMethod) throws MessagingException {

        // ---- Build Thymeleaf context ----
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("movieTitle", movieTitle);
        ctx.setVariable("cinemaName", cinemaName);
        ctx.setVariable("roomName", roomName);
        ctx.setVariable("showDateTime", showDateTime);
        ctx.setVariable("paymentMethod", paymentMethod);

        // Danh sách ghế & FNB chuyển thẳng cho template lặp
        ctx.setVariable("seats", seats);
        ctx.setVariable("fnbs", fnbs);

        // Khuyến mãi (có thể null)
        if (promotion != null) {
            ctx.setVariable("promotionCode", promotion.code());
            ctx.setVariable("promotionDiscount", promotion.discountAmount());
        } else {
            ctx.setVariable("promotionCode", null);
            ctx.setVariable("promotionDiscount", null);
        }

        // Giá trị hiển thị
        ctx.setVariable("finalPrice", finalPrice);
        ctx.setVariable("finalPriceDisplay",
                finalPrice != null ? String.format("%,.0f", finalPrice) : "0");

        String html = templateEngine.process("booking-ticket", ctx);

        // ---- Gửi mail ----
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(to);
        helper.setSubject("CineHub – Vé xem phim của bạn đã sẵn sàng 🎫");
        helper.setText(html, true);

        mailSender.send(message);
    }
}
