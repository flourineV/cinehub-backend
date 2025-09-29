package com.cinehub.auth.service;

import com.cinehub.auth.dto.*;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.UserRepository;
import com.cinehub.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        // Validate password confirmation
        if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password and confirm password do not match!");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken!");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken!");
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already taken!");
        }

        // Check if national ID already exists
        if (userRepository.existsByNationalId(signUpRequest.getNationalId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "National ID is already taken!");
        }

        // Create new user
        User user = User.builder()
                .email(signUpRequest.getEmail())
                .username(signUpRequest.getUsername())
                .phoneNumber(signUpRequest.getPhoneNumber())
                .nationalId(signUpRequest.getNationalId())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail(),
                savedUser.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new JwtResponse(accessToken, refreshToken.getToken(), "Bearer", new UserResponse(savedUser));
    }

    public JwtResponse signIn(LoginRequest loginRequest, HttpServletRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmailOrPhone(),
                        loginRequest.getPassword()));

        User user = userRepository.findByEmailOrUsernameOrPhoneNumber(loginRequest.getUsernameOrEmailOrPhone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new JwtResponse(accessToken, refreshToken.getToken(), "Bearer", new UserResponse(user));
    }

    public JwtResponse refreshToken(String refreshTokenString, HttpServletRequest request) {
        return refreshTokenService.findByToken(refreshTokenString)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(),
                            user.getRole().name());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

                    // Delete old refresh token
                    refreshTokenService.deleteByToken(refreshTokenString);

                    return new JwtResponse(accessToken, newRefreshToken.getToken(), "Bearer", new UserResponse(user));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Refresh token is not in database!"));
    }

    public void signOut(String refreshTokenString) {
        refreshTokenService.deleteByToken(refreshTokenString);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
