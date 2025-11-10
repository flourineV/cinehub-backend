package com.cinehub.auth.controller;

import com.cinehub.auth.dto.request.SignInRequest;
import com.cinehub.auth.dto.request.SignUpRequest;
import com.cinehub.auth.dto.response.JwtResponse;
import com.cinehub.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.includeAccessTokenInBody:true}")
    private boolean includeAccessTokenInBody;

    @PostMapping("/signup")
    public ResponseEntity<JwtResponse> registerUser(
            @Valid @RequestBody SignUpRequest request,
            HttpServletResponse response) {

        JwtResponse jwtResponse = authService.signUp(request);

        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());
        setAccessTokenCookie(response, jwtResponse.getAccessToken());

        JwtResponse.JwtResponseBuilder bodyBuilder = JwtResponse.builder()
                .tokenType("Bearer")
                .user(jwtResponse.getUser());

        if (includeAccessTokenInBody) {
            bodyBuilder.accessToken(jwtResponse.getAccessToken());
        }

        return ResponseEntity.ok(bodyBuilder.build());
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response) {

        JwtResponse jwtResponse = authService.signIn(request);

        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());
        setAccessTokenCookie(response, jwtResponse.getAccessToken());

        JwtResponse.JwtResponseBuilder bodyBuilder = JwtResponse.builder()
                .tokenType("Bearer")
                .user(jwtResponse.getUser());

        if (includeAccessTokenInBody) {
            bodyBuilder.accessToken(jwtResponse.getAccessToken());
        }

        return ResponseEntity.ok(bodyBuilder.build());
    }

    @PostMapping("/signout")
    public ResponseEntity<String> logoutUser(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            authService.signOut(refreshToken);
        }

        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);

        return ResponseEntity.ok("Log out successful!");
    }

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);
        response.addCookie(cookie);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("accessToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
