package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.TokenRefreshRequest;
import com.cinehub.auth.dto.response.JwtResponse;
import com.cinehub.auth.dto.response.UserResponse;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.service.RefreshTokenService;
import com.cinehub.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "Refresh token invalid"));

                String roleName = refreshToken.getUser().getRole() != null
                                ? refreshToken.getUser().getRole().getName()
                                : "guest";

                String newAccessToken = jwtUtil.generateAccessToken(
                                refreshToken.getUser().getId(),
                                roleName);

                RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(refreshToken.getUser());

                refreshTokenService.deleteByToken(requestToken);

                JwtResponse jwtResponse = JwtResponse.builder()
                                .tokenType("Bearer")
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken.getToken())
                                .user(new UserResponse(refreshToken.getUser()))
                                .build();

                return ResponseEntity.ok(jwtResponse);
        }
}
