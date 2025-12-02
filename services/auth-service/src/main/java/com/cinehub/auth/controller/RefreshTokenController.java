package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.TokenRefreshRequest;
import com.cinehub.auth.dto.response.JwtResponse;
import com.cinehub.auth.dto.response.UserResponse;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.service.RefreshTokenService;
import com.cinehub.auth.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.extensions.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Token Refresh")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

        private final RefreshTokenService refreshTokenService;
        private final JwtUtil jwtUtil;

        @Operation(summary = "Refresh access token", description = "Generate new access token and refresh token using old valid refresh token.", extensions = @Extension(name = "x-order", properties = {
                        @ExtensionProperty(name = "order", value = "7")
        }))
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
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
