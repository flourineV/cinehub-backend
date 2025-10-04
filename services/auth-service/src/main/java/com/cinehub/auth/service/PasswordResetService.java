package com.cinehub.auth.service;

import com.cinehub.auth.dto.request.ResetPasswordRequest;
import com.cinehub.auth.entity.PasswordResetToken;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.PasswordResetTokenRepository;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.passwordResetDuration}")
    private int passwordResetDuration;

    public PasswordResetToken createToken(User user) {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetDuration))
                .build();
        return passwordResetTokenRepository.save(token);
    }

    public User validateToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedAtIsNull(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token"));
        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token expired");
        }
        return resetToken.getUser();
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedAtIsNull(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.markTokenAsUsed(request.getToken(), LocalDateTime.now());
    }

    public void deleteExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
