package com.cinehub.auth.service;

import com.cinehub.auth.dto.request.SignInRequest;
import com.cinehub.auth.dto.request.SignUpRequest;
import com.cinehub.auth.dto.response.JwtResponse;
import com.cinehub.auth.dto.response.UserResponse;
import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.entity.Role;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.RoleRepository;
import com.cinehub.auth.repository.UserRepository;
import com.cinehub.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public JwtResponse signUp(SignUpRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password and confirm password do not match!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken!");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken!");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number is already taken!");
        }
        if (userRepository.existsByNationalId(request.getNationalId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "National ID is already taken!");
        }

        Role defaultRole = roleRepository.findByName("customer")
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .phoneNumber(request.getPhoneNumber())
                .nationalId(request.getNationalId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(defaultRole)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getRole().getName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new JwtResponse(accessToken, refreshToken.getToken(), "Bearer", new UserResponse(savedUser));
    }

    public JwtResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmailOrUsernameOrPhoneNumber(request.getUsernameOrEmailOrPhone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().getName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new JwtResponse(accessToken, refreshToken.getToken(), "Bearer", new UserResponse(user));
    }

    public void signOut(String refreshTokenString) {
        refreshTokenService.deleteByToken(refreshTokenString);
    }
}
