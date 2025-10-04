package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.TokenRefreshRequest;
import com.cinehub.auth.dto.response.JwtResponse;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.service.RefreshTokenService;
import com.cinehub.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/refreshtoken")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalid"));

        // sinh access token mới
        String newAccessToken = jwtUtil.generateAccessToken(
                refreshToken.getUser().getId(),
                refreshToken.getUser().getRole().name());

        // sinh refresh token mới
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(refreshToken.getUser());

        // xóa refresh token cũ
        refreshTokenService.deleteByToken(requestToken);

        JwtResponse jwtResponse = JwtResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .user(new com.cinehub.auth.dto.response.UserResponse(refreshToken.getUser()))
                .build();

        return ResponseEntity.ok(jwtResponse);
    }
}
