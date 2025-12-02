package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.ResetPasswordRequest;
import com.cinehub.auth.dto.request.ForgotPasswordRequest;
import com.cinehub.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Password Reset")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Send OTP to email", description = "Send OTP to the user email for password recovery.", extensions = @Extension(name = "x-order", properties = {
            @ExtensionProperty(name = "order", value = "4")
    }))
    @ApiResponse(responseCode = "200", description = "OTP sent successfully")
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendOtp(request.getEmail());
        return ResponseEntity.ok("OTP has been sent to your email!");
    }

    @Operation(summary = "Resend OTP", description = "Resend a new OTP to the user email.", extensions = @Extension(name = "x-order", properties = {
            @ExtensionProperty(name = "order", value = "5")
    }))
    @ApiResponse(responseCode = "200", description = "OTP resent successfully")
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.resendOtp(request.getEmail());
        return ResponseEntity.ok("A new OTP has been sent to your email!");
    }

    @Operation(summary = "Reset password", description = "Reset user password using OTP verification.", extensions = @Extension(name = "x-order", properties = {
            @ExtensionProperty(name = "order", value = "6")
    }))
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully!");
    }
}
