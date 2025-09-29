package com.cinehub.auth.controller;

import com.cinehub.auth.dto.*;
import com.cinehub.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @Value("${app.auth.includeAccessTokenInBody:true}")
    private boolean includeAccessTokenInBody;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        JwtResponse jwtResponse = authService.signUp(signUpRequest, request);

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());
        setAccessTokenCookie(response, jwtResponse.getAccessToken());

        JwtResponse.JwtResponseBuilder responseBodyBuilder = JwtResponse.builder()
                .tokenType("Bearer")
                .user(jwtResponse.getUser());

        if (includeAccessTokenInBody) {
            responseBodyBuilder.accessToken(jwtResponse.getAccessToken());
        }

        JwtResponse responseBody = responseBodyBuilder.build();
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/signin")
    // User can login with email, username, or phone number
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        JwtResponse jwtResponse = authService.signIn(loginRequest, request);

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());

        JwtResponse.JwtResponseBuilder responseBodyBuilder = JwtResponse.builder()
                .tokenType("Bearer")
                .user(jwtResponse.getUser());

        if (includeAccessTokenInBody) {
            responseBodyBuilder.accessToken(jwtResponse.getAccessToken());
        }

        JwtResponse responseBody = responseBodyBuilder.build();
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest, HttpServletResponse response) {
        // Get refresh token from HttpOnly cookie
        String requestRefreshToken = getRefreshTokenFromCookie(httpRequest);

        if (requestRefreshToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token not found in cookie");
        }

        JwtResponse jwtResponse = authService.refreshToken(requestRefreshToken, httpRequest);

        // Set new refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());

        // Return new access token in response body (for Redux)
        JwtResponse responseBody = JwtResponse.builder()
                .tokenType("Bearer")
                .user(jwtResponse.getUser())
                .build();

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            authService.signOut(refreshToken);
        }

        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);

        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(15 * 60); // 15 phút
        response.addCookie(accessTokenCookie);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // Delete cookie
        response.addCookie(refreshTokenCookie);
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("accessToken", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // set true ở production
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // xóa ngay lập tức
        response.addCookie(accessTokenCookie);
    }
}
