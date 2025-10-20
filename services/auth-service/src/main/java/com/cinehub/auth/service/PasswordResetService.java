package com.cinehub.auth.service;

import com.cinehub.auth.dto.request.ResetPasswordRequest;
import com.cinehub.auth.entity.PasswordResetOtp;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.PasswordResetOtpRepository;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.otp.expiration-minutes:5}")
    private int otpExpirationMinutes;

    public void sendOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not registered");
        }

        otpRepository.deleteAllByEmail(email);

        String otp = String.format("%06d", new Random().nextInt(1000000));

        PasswordResetOtp otpEntity = PasswordResetOtp.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();

        otpRepository.save(otpEntity);

        // Gửi email OTP
        emailService.sendEmail(
                email,
                "CineHub - Xác thực đặt lại mật khẩu",
                "Mã OTP của bạn là: " + otp + "\n\n" +
                        "Mã này sẽ hết hạn sau " + otpExpirationMinutes + " phút.");
    }

    public void resendOtp(String email) {
        Optional<PasswordResetOtp> existingOtp = otpRepository.findLatestValidOtp(email, LocalDateTime.now());

        if (existingOtp.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP vẫn còn hiệu lực, hãy thử lại sau.");
        }

        sendOtp(email);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp otpEntity = otpRepository.findByEmailAndOtp(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP"));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP sau khi dùng
        otpRepository.deleteAllByEmail(request.getEmail());
    }

    public void deleteExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }
}
