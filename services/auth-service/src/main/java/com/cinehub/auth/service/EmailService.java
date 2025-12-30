package com.cinehub.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        log.info("📧 Preparing to send email to: {}, subject: {}", to, subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        log.info("📤 Sending email...");
        mailSender.send(message);
        log.info("✅ Email sent successfully to: {}", to);
    }

    public void sendEmailVerification(String to, String verificationCode, String language) {
        String subject = "vi".equals(language) 
            ? "Xác thực email - CineHub" 
            : "Email Verification - CineHub";
        
        String content = buildVerificationEmailContent(verificationCode, language);
        
        sendEmail(to, subject, content);
    }

    private String buildVerificationEmailContent(String verificationCode, String language) {
        if ("vi".equals(language)) {
            return String.format("""
                🎬 CineHub - Xác thực email
                
                Xin chào!
                
                Cảm ơn bạn đã đăng ký tài khoản CineHub. Để hoàn tất quá trình đăng ký, vui lòng sử dụng mã xác thực dưới đây:
                
                MÃ XÁC THỰC: %s
                
                ⏰ Mã này có hiệu lực trong 10 phút.
                ⚠️ Không chia sẻ mã này với bất kỳ ai.
                
                Sau khi xác thực thành công, bạn có thể:
                🎫 Đặt vé xem phim online
                🍿 Đặt combo bắp nước
                🎁 Nhận ưu đãi độc quyền
                ⭐ Tích điểm thành viên
                
                Trân trọng,
                Đội ngũ CineHub
                
                📧 Email: support@cinehub.vn
                📞 Hotline: 1900-xxxx
                
                Email này được gửi tự động, vui lòng không trả lời.
                """, verificationCode);
        } else {
            return String.format("""
                🎬 CineHub - Email Verification
                
                Hello!
                
                Thank you for signing up for CineHub! To complete your registration, please use the verification code below:
                
                VERIFICATION CODE: %s
                
                ⏰ This code is valid for 10 minutes.
                ⚠️ Do not share this code with anyone.
                
                After successful verification, you can:
                🎫 Book movie tickets online
                🍿 Order popcorn & drinks
                🎁 Receive exclusive offers
                ⭐ Earn loyalty points
                
                Best regards,
                CineHub Team
                
                📧 Email: support@cinehub.vn
                📞 Hotline: 1900-xxxx
                
                This is an automated email, please do not reply.
                """, verificationCode);
        }
    }
}
