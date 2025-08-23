package com.cinehub.auth.service;

import com.cinehub.auth.dto.*;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.UserRepository;
import com.cinehub.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    public JwtResponse signUp(SignUpRequest signUpRequest, HttpServletRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already taken!");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFullName(signUpRequest.getFullName());
        user.setRole(User.Role.USER);
        
        User savedUser = userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String userAgent = request.getHeader("User-Agent");
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser, userAgent);
        
        return new JwtResponse(accessToken, refreshToken.getToken(), new UserResponse(savedUser));
    }
    
    public JwtResponse signIn(LoginRequest loginRequest, HttpServletRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        
        // Get user
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String userAgent = request.getHeader("User-Agent");
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent);
        
        return new JwtResponse(accessToken, refreshToken.getToken(), new UserResponse(user));
    }
    
    public JwtResponse refreshToken(String refreshTokenString, HttpServletRequest request) {
        return refreshTokenService.findByToken(refreshTokenString)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
                    String userAgent = request.getHeader("User-Agent");
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, userAgent);
                    
                    // Delete old refresh token
                    refreshTokenService.deleteByToken(refreshTokenString);
                    
                    return new JwtResponse(accessToken, newRefreshToken.getToken(), new UserResponse(user));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
    
    public void signOut(String refreshTokenString) {
        refreshTokenService.deleteByToken(refreshTokenString);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
