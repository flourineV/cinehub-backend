package com.cinehub.profile.controller;

import com.cinehub.profile.dto.request.RankRequest;
import com.cinehub.profile.dto.response.RankResponse;
import com.cinehub.profile.service.UserRankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/ranks")
@RequiredArgsConstructor
public class UserRankController {

    private final UserRankService rankService;

    @PostMapping
    public ResponseEntity<RankResponse> createRank(@Valid @RequestBody RankRequest request) {
        return ResponseEntity.ok(rankService.createRank(request));
    }

    @GetMapping
    public ResponseEntity<List<RankResponse>> getAllRanks() {
        return ResponseEntity.ok(rankService.getAllRanks());
    }

    @GetMapping("/{rankId}")
    public ResponseEntity<RankResponse> getRankById(@PathVariable UUID rankId) {
        return ResponseEntity.ok(rankService.getRankById(rankId));
    }

    @DeleteMapping("/{rankId}")
    public ResponseEntity<Void> deleteRank(@PathVariable UUID rankId) {
        rankService.deleteRank(rankId);
        return ResponseEntity.noContent().build();
    }
}
