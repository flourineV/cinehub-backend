package com.cinehub.notification.service;

import org.springframework.core.io.ClassPathResource;
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
            String reason,
            String language) throws MessagingException {

        boolean isEnglish = "en".equalsIgnoreCase(language);

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("bookingId", bookingId);
        ctx.setVariable("refundAmount", refundAmount);
        ctx.setVariable("reason", reason);

        ctx.setVariable("isVoucher", "VOUCHER".equalsIgnoreCase(refundMethod));
        ctx.setVariable("isCounter", "COUNTER".equalsIgnoreCase(refundMethod));

        // Select template based on language
        String templateName = isEnglish ? "booking-refund-en" : "booking-refund";
        String html = templateEngine.process(templateName, ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        // Set subject based on language
        String subject = isEnglish 
            ? "CineHub – Refund Notification / Ticket Cancellation" 
            : "CineHub – Thông báo hoàn tiền / Hủy vé";
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);

        log.info("Refund email sent to {} (language: {})", to, language);
    }

    public void sendBookingTicketEmail(
            String to,
            String userName,
            UUID bookingId,
            String bookingCode,
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
            String paymentMethod,
            String language) throws MessagingException {

        boolean isEnglish = "en".equalsIgnoreCase(language);

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("bookingCode", bookingCode);
        ctx.setVariable("movieTitle", movieTitle);
        ctx.setVariable("cinemaName", cinemaName);
        ctx.setVariable("roomName", roomName);
        ctx.setVariable("showDateTime", showDateTime);
        ctx.setVariable("paymentMethod", paymentMethod);

        ctx.setVariable("seats", seats);
        ctx.setVariable("fnbs", fnbs);
        ctx.setVariable("rankName", rankName != null ? rankName : (isEnglish ? "No rank" : "Chưa có hạng"));
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

        // Select template based on language
        String templateName = isEnglish ? "booking-ticket-en" : "booking-ticket";
        String html = templateEngine.process(templateName, ctx);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        // Set subject based on language
        String subject = isEnglish 
            ? "CineHub – Your movie ticket is ready" 
            : "CineHub – Vé xem phim của bạn đã sẵn sàng";
        helper.setSubject(subject);
        helper.setText(html, true);

        ClassPathResource logo = new ClassPathResource("templates/LogoFullfinal.png");

        // Skip logo - not needed for email
        // if (logo.exists()) {
        //     helper.addInline("logoImage", logo);
        // }

        mailSender.send(message);

        log.info("Booking ticket email sent to {} (language: {})", to, language);
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
            List<com.cinehub.notification.dto.request.FnbOrderConfirmationRequest.FnbItemDetail> items,
            String language)
            throws MessagingException {

        boolean isEnglish = "en".equalsIgnoreCase(language);

        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("orderCode", orderCode);
        ctx.setVariable("totalAmount", totalAmount);
        ctx.setVariable("items", items);

        // Select template based on language
        String templateName = isEnglish ? "fnb-order-confirmation-en" : "fnb-order-confirmation";
        String html = templateEngine.process(templateName, ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        // Set subject based on language
        String subject = isEnglish 
            ? "CineHub - Food & Beverage Order Confirmation #" + orderCode
            : "CineHub - Xác nhận đơn hàng bắp nước #" + orderCode;
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);

        log.info("FnB order confirmation email sent to {} (language: {})", to, language);
    }
}
