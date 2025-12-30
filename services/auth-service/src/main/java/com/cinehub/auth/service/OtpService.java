package com.cinehub.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {
    
    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    // In-memory storage for OTP codes
    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    // In-memory storage for verified emails (before user registration)
    private final ConcurrentHashMap<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();
    
    public String generateAndStoreOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Store OTP with expiry time
        otpStorage.put(email.toLowerCase(), new OtpData(otp, expiryTime));
        
        log.info("📧 Generated OTP for email: {} (expires at: {})", email, expiryTime);
        return otp;
    }
    
    public boolean verifyOtp(String email, String otp) {
        String emailKey = email.toLowerCase();
        OtpData otpData = otpStorage.get(emailKey);
        
        if (otpData == null) {
            log.warn("❌ No OTP found for email: {}", email);
            return false;
        }
        
        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(emailKey); // Clean up expired OTP
            log.warn("⏰ OTP expired for email: {}", email);
            return false;
        }
        
        // Check if OTP matches
        if (!otpData.otp.equals(otp)) {
            log.warn("❌ Invalid OTP for email: {}", email);
            return false;
        }
        
        // OTP is valid, remove it and mark email as verified
        otpStorage.remove(emailKey);
        verifiedEmails.put(emailKey, LocalDateTime.now().plusHours(1)); // Valid for 1 hour after verification
        log.info("✅ OTP verified successfully for email: {}", email);
        return true;
    }
    
    public boolean isEmailVerified(String email) {
        String emailKey = email.toLowerCase();
        LocalDateTime verificationTime = verifiedEmails.get(emailKey);
        
        if (verificationTime == null) {
            return false;
        }
        
        // Check if verification is still valid (within 1 hour)
        if (LocalDateTime.now().isAfter(verificationTime)) {
            verifiedEmails.remove(emailKey); // Clean up expired verification
            log.info("⏰ Email verification expired for: {}", email);
            return false;
        }
        
        return true;
    }
    
    public void markEmailAsUsed(String email) {
        // Remove from verified emails after successful registration
        verifiedEmails.remove(email.toLowerCase());
        log.info("✅ Email verification used for registration: {}", email);
    }
    
    public void removeOtp(String email) {
        otpStorage.remove(email.toLowerCase());
        log.info("🗑️ Removed OTP for email: {}", email);
    }
    
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up expired OTPs
        otpStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
        
        // Clean up expired email verifications
        verifiedEmails.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        
        log.info("🧹 Cleaned up expired OTPs and email verifications");
    }
    
    private String generateOtp() {
        return String.format("%06d", random.nextInt(1000000));
    }
    
    // Inner class to store OTP data
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;
        
        OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}