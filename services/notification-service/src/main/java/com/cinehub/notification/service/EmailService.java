package com.cinehub.notification.service;

import com.cinehub.notification.dto.email.PromotionEmailData;
import com.cinehub.notification.events.dto.SeatDetail;
import com.cinehub.notification.events.dto.FnbDetail;
import com.cinehub.notification.events.dto.PromotionDetail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine templateEngine;

    @Value("${cinehub.admin.email}")
    private String adminEmail;

    public void sendRefundEmail(
            String to,
            String userName,
            UUID bookingId,
            BigDecimal refundAmount,
            String refundMethod, // "VOUCHER" or "COUNTER"
            String reason) throws MessagingException {

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("bookingId", bookingId);
        ctx.setVariable("refundAmount", refundAmount);
        ctx.setVariable("reason", reason);

        ctx.setVariable("isVoucher", "VOUCHER".equalsIgnoreCase(refundMethod));
        ctx.setVariable("isCounter", "COUNTER".equalsIgnoreCase(refundMethod));

        String html = templateEngine.process("booking-refund", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        helper.setSubject("CineHub – Thông báo hoàn tiền / Hủy vé");
        helper.setText(html, true);

        mailSender.send(message);

        log.info("Refund email sent to {}", to);
    }

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
            String paymentMethod) throws MessagingException {

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
        helper.setSubject("CineHub – Vé xem phim của bạn đã sẵn sàng");
        helper.setText(html, true);

        mailSender.send(message);

        log.info("Booking ticket email sent to {}", to);
    }

    public void sendPromotionEmail(
            String to,
            String promotionCode,
            String discountType,
            BigDecimal discountValue,
            String discountValueDisplay,
            String description,
            String promoDisplayUrl,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String usageRestriction,
            String actionUrl,
            boolean isOneTimeUse) throws MessagingException {

        Context ctx = new Context();
        ctx.setVariable("name", "Quý khách");

        PromotionEmailData promotion = new PromotionEmailData(
                promotionCode,
                description,
                discountValue,
                startDate,
                endDate,
                isOneTimeUse,
                discountType);

        ctx.setVariable("promotion", promotion);
        ctx.setVariable("discountValueDisplay", discountValueDisplay);
        ctx.setVariable("promoDisplayUrl", promoDisplayUrl);
        ctx.setVariable("usageRestriction", usageRestriction);
        ctx.setVariable("actionUrl", actionUrl != null ? actionUrl : "https://cinehub.com");

        String html = templateEngine.process("promotion", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        helper.setSubject("CineHub - Ưu đãi đặc biệt dành cho bạn");
        helper.setText(html, true);

        mailSender.send(message);

        log.info("Promotion email sent to {}", to);
    }

    public void sendContactEmail(String name, String email, String messageContent) throws MessagingException {

        String emailBody = "CineHub Contact Form Submission\n\n" +
                "Name: " + name + "\n" +
                "Email: " + email + "\n" +
                "Message:\n" + messageContent + "\n\n" +
                "Received at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(adminEmail);
        helper.setSubject("CineHub - Contact Form: " + name);
        helper.setReplyTo(email);
        helper.setText(emailBody, false);

        mailSender.send(message);

        log.info("Contact email sent from {} ({})", name, email);
    }

    public void sendFnbOrderConfirmationEmail(
            String to,
            String userName,
            String orderCode,
            BigDecimal totalAmount,
            List<com.cinehub.notification.dto.request.FnbOrderConfirmationRequest.FnbItemDetail> items)
            throws MessagingException {

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("orderCode", orderCode);
        ctx.setVariable("totalAmount", totalAmount);
        ctx.setVariable("items", items);

        String html = templateEngine.process("fnb-order-confirmation", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        helper.setSubject("CineHub - Xác nhận đơn hàng bắp nước #" + orderCode);
        helper.setText(html, true);

        mailSender.send(message);

        log.info("FnB order confirmation email sent to {}", to);
    }
}
