package com.cinehub.auth.service;

import com.cinehub.auth.entity.RefreshToken;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.RefreshTokenRepository;
import com.cinehub.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class RefreshTokenService {
    
    @Value("${app.jwtRefreshExpirationMs:604800000}") // 7 days default
    private Long refreshTokenDurationMs;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public RefreshToken createRefreshToken(User user, String userAgent) {
        // Delete existing refresh token for this user and user agent
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserAndUserAgent(user, userAgent);
        existingToken.ifPresent(refreshTokenRepository::delete);
        
        // Create new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtUtil.generateRefreshToken());
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
    
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
    
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
    
    @Scheduled(fixedRate = 86400000) // Run every 24 hours
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
