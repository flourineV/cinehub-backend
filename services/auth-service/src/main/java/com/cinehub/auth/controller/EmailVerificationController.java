package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.SendVerificationRequest;
import com.cinehub.auth.dto.request.VerifyEmailRequest;
import com.cinehub.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Email Verification")
@RestController
@RequestMapping("/api/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "Send verification code to email")
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendVerificationCode(
            @Valid @RequestBody SendVerificationRequest request) {
        
        emailVerificationService.sendVerificationCode(request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Verification code sent successfully",
                "email", request.getEmail()
        ));
    }

    @Operation(summary = "Verify email with code")
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        
        boolean verified = emailVerificationService.verifyEmail(request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully",
                "verified", verified,
                "email", request.getEmail()
        ));
    }

    @Operation(summary = "Check if email is verified")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkEmailStatus(
            @RequestParam String email) {
        
        boolean verified = emailVerificationService.isEmailVerified(email);
        
        return ResponseEntity.ok(Map.of(
                "email", email,
                "verified", verified
        ));
    }
}