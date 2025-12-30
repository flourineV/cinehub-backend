package com.cinehub.auth.service;

import com.cinehub.auth.dto.request.SendVerificationRequest;
import com.cinehub.auth.dto.request.VerifyEmailRequest;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    
    // Track last send time to prevent duplicate sends
    private final ConcurrentHashMap<String, LocalDateTime> lastSendTime = new ConcurrentHashMap<>();
    private static final int MIN_SEND_INTERVAL_SECONDS = 5; // Minimum 5 seconds between sends

    @Transactional
    public void sendVerificationCode(SendVerificationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("📧 Starting to send verification code to: {}", email);
        
        // Check for duplicate sends within minimum interval
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSent = lastSendTime.get(email);
        if (lastSent != null && lastSent.plusSeconds(MIN_SEND_INTERVAL_SECONDS).isAfter(now)) {
            log.warn("⏰ Duplicate send attempt blocked for email: {} (last sent: {})", email, lastSent);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another verification code");
        }
        
        // Kiểm tra email đã được đăng ký và verified chưa
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && existingUser.get().isEmailVerified()) {
            log.warn("❌ Email is already verified: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified");
        }
        
        // Update last send time before processing
        lastSendTime.put(email, now);
        
        // Tạo và lưu OTP vào memory
        String otp = otpService.generateAndStoreOtp(email);
        log.info("🔑 Generated OTP for email: {}", email);
        
        // Gửi email trực tiếp
        try {
            log.info("📤 Attempting to send email to: {}", email);
            emailService.sendEmailVerification(email, otp, "vi"); // Default Vietnamese
            log.info("✅ Successfully sent verification code to email: {}", email);
        } catch (Exception e) {
            log.error("❌ Failed to send verification email to: {} - Error: {}", email, e.getMessage(), e);
            // Remove OTP and last send time if email sending failed
            otpService.removeOtp(email);
            lastSendTime.remove(email);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send verification email: " + e.getMessage());
        }
    }

    @Transactional
    public boolean verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String code = request.getVerificationCode();
        
        // Verify OTP using OtpService
        if (!otpService.verifyOtp(email, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
        }
        
        // Cập nhật trạng thái email verified cho user (nếu đã tồn tại)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            userRepository.save(user);
            log.info("✅ Email verified for existing user: {}", email);
        }
        
        log.info("✅ Email verification successful: {}", email);
        return true;
    }
    
    public boolean isEmailVerified(String email) {
        // First check if email is verified in memory (before registration)
        if (otpService.isEmailVerified(email)) {
            return true;
        }
        
        // Then check if user exists and is verified (after registration)
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        return userOpt.map(User::isEmailVerified).orElse(false);
    }
}