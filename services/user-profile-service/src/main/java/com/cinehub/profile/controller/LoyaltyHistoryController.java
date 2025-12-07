package com.cinehub.profile.controller;

import com.cinehub.profile.dto.response.PagedLoyaltyHistoryResponse;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.LoyaltyHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/loyalty-history")
@RequiredArgsConstructor
public class LoyaltyHistoryController {

    private final LoyaltyHistoryService loyaltyHistoryService;

    @GetMapping("/{userId}")
    public ResponseEntity<PagedLoyaltyHistoryResponse> getUserLoyaltyHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        AuthChecker.requireAuthenticated();
        
        PagedLoyaltyHistoryResponse response = loyaltyHistoryService.getUserLoyaltyHistory(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
