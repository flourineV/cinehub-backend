package com.cinehub.notification.controller;

import com.cinehub.notification.dto.request.ContactRequest;
import com.cinehub.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendContactForm(@Valid @RequestBody ContactRequest request) {
        try {
            emailService.sendContactEmail(
                    request.getName(),
                    request.getEmail(),
                    request.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact form submitted successfully. We will get back to you soon!");

            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to send contact form. Please try again later.");

            return ResponseEntity.internalServerError().body(error);
        }
    }
}
