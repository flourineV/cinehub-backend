package com.cinehub.notification.service;

import com.cinehub.notification.events.dto.SeatDetail;
import com.cinehub.notification.events.dto.FnbDetail;
import com.cinehub.notification.events.dto.PromotionDetail;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine templateEngine;

    public void sendBookingTicketEmail(
            String to,
            String userName,
            UUID bookingId,
            String movieTitle,
            String cinemaName,
            String roomName,
            String showDateTime,
            List<SeatDetail> seats,
            List<FnbDetail> fnbs,
            PromotionDetail promotion,
            String rankName,                    
            BigDecimal rankDiscountAmount,      
            BigDecimal totalPrice,              
            BigDecimal finalPrice,
            String paymentMethod
    ) throws MessagingException {

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("movieTitle", movieTitle);
        ctx.setVariable("cinemaName", cinemaName);
        ctx.setVariable("roomName", roomName);
        ctx.setVariable("showDateTime", showDateTime);
        ctx.setVariable("paymentMethod", paymentMethod);

        ctx.setVariable("seats", seats);
        ctx.setVariable("fnbs", fnbs);

        ctx.setVariable("rankName", rankName != null ? rankName : "Chưa có hạng");
        ctx.setVariable("rankDiscountAmount", rankDiscountAmount != null ? rankDiscountAmount : BigDecimal.ZERO);

        if (promotion != null) {
            ctx.setVariable("promotionCode", promotion.code());
            ctx.setVariable("promotionDiscount", promotion.discountAmount());
        } else {
            ctx.setVariable("promotionCode", null);
            ctx.setVariable("promotionDiscount", BigDecimal.ZERO);
        }

        ctx.setVariable("totalPrice", totalPrice);
        ctx.setVariable("finalPrice", finalPrice);

        String html = templateEngine.process("booking-ticket", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(to);
        helper.setSubject("CineHub – Vé xem phim của bạn đã sẵn sàng 🎫");
        helper.setText(html, true);

        mailSender.send(message);
    }
}
