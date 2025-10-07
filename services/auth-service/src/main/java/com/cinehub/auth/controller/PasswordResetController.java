package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.ForgotPasswordRequest;
import com.cinehub.auth.dto.request.ResetPasswordRequest;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.UserRepository;
import com.cinehub.auth.service.PasswordResetService;
import com.cinehub.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        var token = passwordResetService.createToken(user);

        // Gửi email cho user
        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Request",
                "Click the link to reset your password: http://localhost:8081/reset-password?token="
                        + token.getToken());

        return ResponseEntity.ok("Password reset link sent to your email!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully!");
    }
}
