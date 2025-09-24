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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    AuthService authService;
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest, 
                                        HttpServletRequest request, 
                                        HttpServletResponse response) {
        JwtResponse jwtResponse = authService.signUp(signUpRequest, request);
        
        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, jwtResponse.getRefreshToken());
        
        // Return access token in response body (for Redux)
        JwtResponse responseBody = JwtResponse.builder()
                .accessToken(jwtResponse.getAccessToken())
                .tokenType("Bearer")
                .user(jwtResponse.getUser())
                .build();
                
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
        
        // Return access token in response body (for Redux)
        JwtResponse responseBody = JwtResponse.builder()
                .accessToken(jwtResponse.getAccessToken())
                .tokenType("Bearer")
                .user(jwtResponse.getUser())
                .build();
                
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
                .accessToken(jwtResponse.getAccessToken())
                .tokenType("Bearer")
                .user(jwtResponse.getUser())
                .build();
                
        return ResponseEntity.ok(responseBody);
    }
    
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // Get refresh token from cookie
        String refreshToken = getRefreshTokenFromCookie(request);
        
        if (refreshToken != null) {
            authService.signOut(refreshToken);
        }
        
        // Clear refresh token cookie
        clearRefreshTokenCookie(response);
        
        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }
    
    // Helper method to set refresh token as HttpOnly cookie
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);
    }
    
    // Helper method to get refresh token from cookie
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
    
    // Helper method to clear refresh token cookie
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // Delete cookie
        response.addCookie(refreshTokenCookie);
    }
}
