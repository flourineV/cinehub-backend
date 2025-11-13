package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.ResetPasswordRequest;
import com.cinehub.auth.dto.request.ForgotPasswordRequest;
import com.cinehub.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendOtp(request.getEmail());
        return ResponseEntity.ok("OTP has been sent to your email!");
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.resendOtp(request.getEmail());
        return ResponseEntity.ok("A new OTP has been sent to your email!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully!");
    }
}
